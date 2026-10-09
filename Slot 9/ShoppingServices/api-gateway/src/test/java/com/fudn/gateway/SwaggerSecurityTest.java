package com.fudn.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
        "services.product.url", "services.order.url", "services.inventory.url"}))
class SwaggerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() { }

    @Test
    void swaggerUiShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
    }

    @Test
    void swaggerConfigShouldListAllServicesWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls.length()").value(3))
                .andExpect(jsonPath("$.urls[*].name", org.hamcrest.Matchers.containsInAnyOrder(
                        "Product Service", "Order Service", "Inventory Service")))
                .andExpect(jsonPath("$.urls[*].url", org.hamcrest.Matchers.containsInAnyOrder(
                        "/aggregate/product-service/v3/api-docs",
                        "/aggregate/order-service/v3/api-docs",
                        "/aggregate/inventory-service/v3/api-docs")));
    }

    @Test
    void aggregateProductDocsShouldBePermittedWithoutToken() throws Exception {
        assertAggregate("product-service");
    }

    @Test
    void aggregateOrderDocsShouldBePermittedWithoutToken() throws Exception {
        assertAggregate("order-service");
    }

    @Test
    void aggregateInventoryDocsShouldBePermittedWithoutToken() throws Exception {
        assertAggregate("inventory-service");
    }

    private void assertAggregate(String service) throws Exception {
        String spec = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"" + service + "\"}}";
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api-docs"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json").withBody(spec)));
        mockMvc.perform(get("/aggregate/" + service + "/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().json(spec));
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api-docs")));
    }

    @Test
    void protectedApiShouldRequireToken() throws Exception {
        for (String path : new String[]{"/api/products", "/api/order", "/api/inventory", "/api/product"}) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void preflightShouldPermitProductUpdateWithoutToken() throws Exception {
        mockMvc.perform(options("/api/products/1")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}
