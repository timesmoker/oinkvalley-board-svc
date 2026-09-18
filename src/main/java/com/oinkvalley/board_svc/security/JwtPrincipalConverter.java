package com.oinkvalley.board_svc.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtPrincipalConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new InvalidBearerTokenException("JWT subject must be a Long", exception);
        }

        List<SimpleGrantedAuthority> authorities = roles(jwt).stream()
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(JwtPrincipalConverter::toAuthority)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    private static List<String> roles(Jwt jwt) {
        try {
            List<String> roles = jwt.getClaimAsStringList("roles");
            return roles == null ? List.of() : roles;
        } catch (IllegalArgumentException exception) {
            throw new InvalidBearerTokenException("JWT roles must be a string list", exception);
        }
    }

    private static String toAuthority(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
