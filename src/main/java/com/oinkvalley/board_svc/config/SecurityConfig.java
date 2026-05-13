package com.oinkvalley.board_svc.config;

import com.oinkvalley.board_svc.db.repository.BoardRepository;
import com.oinkvalley.board_svc.db.repository.CommentRepository;
import com.oinkvalley.board_svc.db.repository.PostRepository;
import com.oinkvalley.board_svc.security.JwtAuthenticationFilter;
import com.oinkvalley.board_svc.security.RestrictedBoardReadRequestMatcher;
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
 * 스프링 시큐리티 필터 체인. 규칙은 위에서 아래로 먼저 매칭되므로
 * {@link RestrictedBoardReadRequestMatcher} 를 일반 {@code GET} 허용보다 앞에 둡니다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var restrictedBoardReads = new RestrictedBoardReadRequestMatcher(
                boardRepository, postRepository, commentRepository);
        // JWT 상태 없음 구성: CSRF 비활성, 세션 미생성
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers(restrictedBoardReads).authenticated()
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
