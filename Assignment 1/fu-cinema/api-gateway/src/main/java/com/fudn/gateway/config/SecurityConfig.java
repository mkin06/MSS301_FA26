package com.fudn.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpStatus.FORBIDDEN, "Access is denied", request.getRequestURI())))
                .authorizeHttpRequests(auth -> auth
                        // ---------- Public ----------
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/customers/register").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/genres/**", "/api/movies/**", "/api/showtimes/**",
                                "/api/bookings/showtimes/**").permitAll()

                        // ---------- customer-service ----------
                        .requestMatchers("/api/customers/me", "/api/customers/me/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/customers/**").hasRole("ADMIN")

                        // ---------- movie-service (ghi) ----------
                        .requestMatchers("/api/rooms/**", "/api/genres/**", "/api/movies/**", "/api/showtimes/**")
                                .hasRole("ADMIN")

                        // ---------- booking-service ----------
                        .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/my").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings", "/api/bookings/report").hasRole("ADMIN")
                        .requestMatchers("/api/bookings/**").authenticated()   // /{id}, /{id}/cancel

                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Invalid or expired access token", request.getRequestURI()))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpStatus.FORBIDDEN, "Access is denied", request.getRequestURI()))
                        .jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    private static void writeError(HttpServletResponse response, HttpStatus status,
                                   String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().write(JsonMapper.builder().build().writeValueAsString(Map.of(
                "timestamp", LocalDateTime.now().toString(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", message, "path", path)));
    }

    /** Verify chu ky HS256 bang CUNG secret voi customer-service; tu kiem tra exp. */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Claim "role": "ADMIN" -> authority "ROLE_ADMIN" de dung hasRole("ADMIN"). */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
