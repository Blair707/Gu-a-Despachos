package com.transporte.guias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de Guías de Despacho.
 *
 * Integra Spring Security con AWS Cognito como IdaaS mediante validación JWT.
 * Todos los endpoints de /api/guias/** requieren un token Bearer válido
 * emitido por el User Pool de Cognito configurado.
 *
 * Variables de entorno necesarias:
 *   COGNITO_JWKS_URI  → https://cognito-idp.<region>.amazonaws.com/<userPoolId>/.well-known/jwks.json
 *   COGNITO_ISSUER    → https://cognito-idp.<region>.amazonaws.com/<userPoolId>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwksUri;

    /**
     * Cadena de filtros de seguridad HTTP.
     * - Desactiva CSRF (API REST stateless).
     * - No usa sesiones HTTP.
     * - Permite acceso libre a Swagger / OpenAPI.
     * - Requiere JWT válido para todos los endpoints de la API.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Swagger UI y OpenAPI (documentación)
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // Health check (opcional)
                .requestMatchers("/actuator/health").permitAll()
                // Todos los endpoints de la API requieren autenticación
                .requestMatchers("/api/guias/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        return http.build();
    }

    /**
     * Decodificador JWT que valida tokens contra el JWKS de AWS Cognito.
     * La validación incluye firma, expiración e issuer automáticamente.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}
