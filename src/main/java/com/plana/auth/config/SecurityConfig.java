package com.plana.auth.config;

import com.plana.auth.service.OAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정 클래스
 * OAuth2 소셜 로그인과 JWT 기반 인증 시스템 구성
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Spring Security 필터 체인 설정
     * HTTP 보안, CORS, OAuth2 로그인, JWT 인증 등을 구성
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용으로 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 사용 안 함 (JWT 사용)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // HTTP 요청 인증 설정
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 엔드포인트
                .requestMatchers(
                    "/",
                    "/api/test/**",           // 테스트 API (기존)
                    "/api/download/**",       // 파일 다운로드 API (기존)
                    "/oauth2/**",             // OAuth2 로그인 엔드포인트
                    "/login/**",              // 로그인 관련 엔드포인트
                    "/auth/**",               // 인증 관련 엔드포인트 (프론트엔드)
                    "/api/auth/status",       // 상태 확인 API
                    "/api/auth/hello",        // 테스트 API
                    "/api/auth/test-jwt",     // JWT 테스트 API
                    "/api/auth/signup",       // 일반 회원가입 API
                    "/api/auth/login",        // 일반 로그인 API
                    "/error",                 // 에러 페이지
                    "/v3/api-docs/**",        // Swaager가 자동 생성하는 API 명세 JSON 데이터가 위치하는 기본 URL 경로
                    "/swagger-ui/**",         // Swagger UI관련 정적리소스가 위치하는 경로
                    "/swagger-ui.html"        // Swagger UI를 열기 위한 메인 HTML 페이지 URL

                ).permitAll()
                
                // 관리자만 접근 가능한 엔드포인트
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 그 외 모든 요청은 인증 필요 (JWT 토큰 필요)
                .anyRequest().authenticated()
            )
            
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                // 소셜 로그인 사용자 정보 처리 서비스 설정
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService))
                
                // 로그인 성공 시 핸들러 설정
                .successHandler(oAuth2SuccessHandler)
                
                // 로그인 실패 시 처리 (선택사항)
                .failureHandler((request, response, exception) -> {
                    // 로그인 실패 시 프론트엔드 에러 페이지로 리다이렉트
                    response.sendRedirect("/auth/callback?error=oauth2_login_failed");
                })
            );

        return http.build();
    }

    /**
     * CORS 설정
     * 프론트엔드에서 백엔드 API 호출을 위한 CORS 정책 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 오리진 (프론트엔드 주소)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",     // React 기본 포트
            "http://localhost:5173",     // Vite 기본 포트
            "http://localhost:5174",     // Vite 대체 포트
            "http://localhost:80",       // HTTP
            "http://localhost",          // 포트 없는 localhost
            "http://localhost:443",      // HTTPS 포트
            "https://localhost:443",     // HTTPS
            "http://hoonee-math.info",   // 프로덕션 도메인 (HTTP)
            "https://hoonee-math.info"   // 프로덕션 도메인 (HTTPS)
        ));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));
        
        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // 노출할 헤더 (프론트엔드에서 읽을 수 있는 헤더)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Disposition",  // 파일 다운로드용
            "X-Total-Count"         // 페이징용
        ));
        
        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈 설정
     * BCrypt 해시 알고리즘 사용 (솔트 자동 생성)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
        );
    }
}
