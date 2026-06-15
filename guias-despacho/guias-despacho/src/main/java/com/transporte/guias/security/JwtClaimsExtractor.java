package com.transporte.guias.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Utilidad para extraer claims del JWT de AWS Cognito.
 *
 * AWS Cognito incluye los siguientes claims relevantes en el access token:
 *   - sub          : UUID único del usuario
 *   - username     : Nombre de usuario en el User Pool
 *   - cognito:groups: Lista de grupos a los que pertenece el usuario
 *   - email        : Correo electrónico (si el scope lo permite)
 */
@Component
public class JwtClaimsExtractor {

    /**
     * Retorna el JWT del contexto de seguridad actual.
     */
    public Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    /**
     * Retorna el username del usuario autenticado (claim "username" de Cognito).
     */
    public String getUsername() {
        Jwt jwt = getJwt();
        if (jwt == null) return null;
        // Cognito usa "username" en el access token
        String username = jwt.getClaimAsString("username");
        if (username == null) {
            // Fallback al "sub" (UUID del usuario)
            username = jwt.getSubject();
        }
        return username;
    }

    /**
     * Retorna el subject (sub) del JWT, que en Cognito es el UUID del usuario.
     */
    public String getSub() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getSubject() : null;
    }

    /**
     * Retorna el email del usuario si está disponible en el token.
     */
    public String getEmail() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getClaimAsString("email") : null;
    }
}
