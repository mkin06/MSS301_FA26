package com.fudn.gateway;

import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {
    private static final String SECRET = "fu-cinema-booking-system-secret-key-2026-mss301";
    private static final HttpServer BACKEND = backend();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    @LocalServerPort int port;

    static HttpServer backend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String result = String.join("|", String.valueOf(exchange.getRequestHeaders().getFirst("X-User-Id")),
                        String.valueOf(exchange.getRequestHeaders().getFirst("X-User-Email")),
                        String.valueOf(exchange.getRequestHeaders().getFirst("X-User-Role")));
                byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    @DynamicPropertySource static void urls(DynamicPropertyRegistry registry) {
        String url = "http://127.0.0.1:" + BACKEND.getAddress().getPort();
        registry.add("services.customer.url", () -> url);
        registry.add("services.movie.url", () -> url);
        registry.add("services.booking.url", () -> url);
    }
    @AfterAll static void stopBackend() { BACKEND.stop(0); }
    String token(String role, Instant expiration) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("customer@example.com").claim("uid", 42L)
                .claim("role", role).issuedAt(expiration.minusSeconds(3600)).expiresAt(expiration).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    HttpResponse<String> get(String path, String token, boolean spoof) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if (token != null) request.header("Authorization", "Bearer "+token);
        if (spoof) request.header("X-User-Id", "99").header("X-User-Email", "admin@example.com").header("X-User-Role", "ADMIN");
        return CLIENT.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void publicRequestStripsAllSpoofedHeaders() throws Exception {
        HttpResponse<String> response = get("/api/movies", null, true);
        assertEquals(200, response.statusCode());
        assertEquals("null|null|null", response.body());
    }
    @Test void verifiedTokenOverridesAllSpoofedHeaders() throws Exception {
        HttpResponse<String> response = get("/api/customers/me", token("CUSTOMER", Instant.now().plusSeconds(600)), true);
        assertEquals(200, response.statusCode());
        assertEquals("42|customer@example.com|CUSTOMER", response.body());
    }
    @Test void missingTokenIsUnauthorized() throws Exception {
        HttpResponse<String> response = get("/api/customers/me", null, false);
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
        assertTrue(response.body().contains("\"path\":\"/api/customers/me\""));
    }
    @Test void malformedTokenIsUnauthorized() throws Exception { assertEquals(401, get("/api/customers/me", "abc.def.ghi", false).statusCode()); }
    @Test void expiredTokenIsUnauthorized() throws Exception { assertEquals(401, get("/api/customers/me", token("CUSTOMER", Instant.now().minusSeconds(120)), false).statusCode()); }
    @Test void customerCannotReadAdminEndpoint() throws Exception { assertEquals(403, get("/api/rooms", token("CUSTOMER", Instant.now().plusSeconds(600)), false).statusCode()); }
    @Test void adminCannotUseCustomerProfile() throws Exception { assertEquals(403, get("/api/customers/me", token("ADMIN", Instant.now().plusSeconds(600)), false).statusCode()); }
    @Test void adminCanReadReport() throws Exception { assertEquals(200, get("/api/bookings/report", token("ADMIN", Instant.now().plusSeconds(600)), false).statusCode()); }
}
