package com.fudn.product_service;

import com.fudn.product_service.repository.IProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Actual Springdoc and controllers; documentation does not need a database.
@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class SwaggerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private IProductRepository repository;

    @Test
    void swaggerUiShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsShouldReturnJson() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("Product Service API"))
                .andExpect(jsonPath("$.info.version").value("v0.0.1"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:9000"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void apiDocsShouldContainProductEndpoints() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/products'].get").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].delete").exists());
    }

    @Test
    void apiPreflightShouldAllowSwaggerOrigin() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header("Origin", "http://localhost:9000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:9000"));
    }
}
