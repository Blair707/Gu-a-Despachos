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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de Guías de Despacho.
 *
 * Integra Spring Security con Azure AD B2C como IdaaS mediante validación JWT.
 *
 * Roles configurados en Azure AD B2C:
 *   - ROLE_ADMIN    → acceso a todos los endpoints
 *   - ROLE_DESCARGA → acceso solo a GET /api/guias/{id}/download
 *
 * Variables requeridas en application.properties:
 *   azure.tenant-id  → ID del tenant de Azure AD B2C
 *   azure.client-id  → ID de la aplicación registrada
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwksUri;

    /**
     * Cadena de filtros de seguridad HTTP.
     *
     * Reglas de acceso:
     * - GET /api/guias/{id}/download → ROLE_ADMIN o ROLE_DESCARGA
     * - Resto de endpoints           → solo ROLE_ADMIN
     * - Swagger UI y actuator        → público
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Swagger UI y OpenAPI
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // Health check
                .requestMatchers("/actuator/health").permitAll()
                // Endpoint de descarga: accesible por ROLE_ADMIN y ROLE_DESCARGA
                .requestMatchers(HttpMethod.GET, "/api/guias/*/download")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_DESCARGA")
                // Todos los demás endpoints: solo ROLE_ADMIN
                .requestMatchers("/api/guias/**")
                    .hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Decodificador JWT que valida tokens contra el JWKS de Azure AD B2C.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }

    /**
     * Convierte los roles del claim "roles" del token JWT de Azure AD B2C
     * en authorities de Spring Security con prefijo ROLE_.
     *
     * Azure AD B2C incluye los App Roles asignados en el claim "roles"
     * del access token como un array de strings (ej: ["ROLE_ADMIN"]).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        // Azure AD B2C pone los roles en el claim "roles"
        authoritiesConverter.setAuthoritiesClaimName("roles");

        // No agregar prefijo adicional porque los valores ya incluyen ROLE_
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
