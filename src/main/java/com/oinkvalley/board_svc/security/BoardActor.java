package com.oinkvalley.board_svc.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 현재 요청 주체. 비로그인은 {@code userId=null}, roles={@code [ANONYMOUS]}.
 * 로그인 사용자는 JWT roles(접두사 {@code ROLE_} 제거)를 그대로 가진다.
 */
public record BoardActor(Long userId, Set<String> roles) {

    public static final String ROLE_ANONYMOUS = "ANONYMOUS";

    private static final BoardActor ANONYMOUS = new BoardActor(null, Set.of(ROLE_ANONYMOUS));

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isAuthorOf(Long authorUserId) {
        return userId != null && userId.equals(authorUserId);
    }

    /** SecurityContext 에서 현재 주체 해석. JwtAuthenticationFilter 가 principal=userId(Long) 로 채운다. */
    public static BoardActor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            return ANONYMOUS;
        }
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                .filter(r -> !r.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        return new BoardActor(userId, roles);
    }
}
