package com.oinkvalley.board_svc.config;

import com.oinkvalley.board_svc.security.JwtAuthenticationFilter;
import com.oinkvalley.board_svc.security.SecurityJsonHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * 스프링 시큐리티 필터 체인. 읽기 경로는 여기서 열어두고,
 * 게시판별 접근 판정은 서비스 계층의 {@code BoardPermissionService} 가 담당합니다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityJsonHandlers securityJsonHandlers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // JWT 상태 없음 구성: CSRF 비활성, 세션 미생성
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                securityJsonHandlers.writeUnauthorized(response))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityJsonHandlers.writeForbidden(response))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/boards/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/comments/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 인가 판단 전에 Bearer 토큰을 파싱해 SecurityContext 를 채움
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
                .build();
    }
}
