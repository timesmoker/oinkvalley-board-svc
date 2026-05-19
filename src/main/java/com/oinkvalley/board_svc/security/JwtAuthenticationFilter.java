package com.oinkvalley.board_svc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * {@code Authorization} 헤더에 Bearer 형식 JWT 가 있으면 검증 후 인증 객체를 넣고, 없거나 형식이 다르면
 * 익명 요청으로 다음 필터에 넘깁니다(해당 경로가 허용이면 그대로 통과).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final SecurityJsonHandlers securityJsonHandlers;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // Bearer 가 아니면 토큰 없음으로 간주 — 여기서 차단하지 않음
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtUtil.ParsedJwt parsed = jwtUtil.parseToken(token);
            List<SimpleGrantedAuthority> authorities = parsed.roles().stream()
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .map(r -> new SimpleGrantedAuthority(toAuthority(r)))
                    .toList();
            var auth = new UsernamePasswordAuthenticationToken(
                    parsed.userId(),
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            securityJsonHandlers.writeInvalidToken(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** JWT에 {@code ROLE_} 접두사 없이 오면 스프링 {@code hasRole} 규약에 맞게 접두사를 붙입니다. */
    private static String toAuthority(String raw) {
        if (raw.startsWith("ROLE_")) {
            return raw;
        }
        return "ROLE_" + raw;
    }
}
