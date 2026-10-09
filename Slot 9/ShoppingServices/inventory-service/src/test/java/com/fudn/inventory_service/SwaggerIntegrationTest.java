package com.fudn.inventory_service;

import com.fudn.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Actual Springdoc and controllers; documentation does not need a database.
@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"})
@AutoConfigureMockMvc
class SwaggerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private InventoryRepository repository;

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
                .andExpect(jsonPath("$.info.title").value("Inventory Service API"))
                .andExpect(jsonPath("$.info.version").value("v0.0.1"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:9000"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void apiDocsShouldContainInventoryEndpoints() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/inventory'].get").exists());
    }

    @Test
    void apiPreflightShouldAllowSwaggerOrigin() throws Exception {
        mockMvc.perform(options("/api/inventory")
                        .header("Origin", "http://localhost:9000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:9000"));
    }
}
