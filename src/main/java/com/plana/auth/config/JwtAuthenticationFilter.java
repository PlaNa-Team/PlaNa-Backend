package com.plana.auth.config;

import com.plana.auth.entity.User;
import com.plana.auth.repository.UserRepository;
import com.plana.auth.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT 토큰 인증 필터
 * Authorization 헤더의 JWT 토큰을 검증하고 Spring Security Context에 인증 정보 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Authorization 헤더에서 JWT 토큰 추출
        String token = getTokenFromRequest(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                // 토큰에서 사용자 ID 추출
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                
                // 사용자 정보 조회
                Optional<User> userOptional = userRepository.findById(userId);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    
                    // 계정이 활성화되어 있는지 확인
                    if (user.getEnabled()) {
                        // Spring Security 인증 객체 생성
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                user,  // Principal (인증된 사용자 정보)
                                null,  // Credentials (비밀번호 등, JWT에서는 불필요)
                                Collections.singletonList(new SimpleGrantedAuthority(user.getRole())) // 권한
                            );
                        
                        // SecurityContext에 인증 정보 설정
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.debug("JWT authentication successful for user: {}", user.getEmail());
                    } else {
                        log.warn("User account is disabled: {}", user.getEmail());
                    }
                } else {
                    log.warn("User not found for token userId: {}", userId);
                }
                
            } catch (Exception e) {
                log.error("JWT authentication failed", e);
                // 인증 실패 시 SecurityContext 초기화
                SecurityContextHolder.clearContext();
            }
        } else if (token != null) {
            log.debug("Invalid JWT token received");
        }
        
        // 다음 필터 실행
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     * Authorization: Bearer eyJ... 형태에서 토큰 부분만 추출
     * @param request HTTP 요청
     * @return JWT 토큰 (없으면 null)
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // "Bearer " 제거하고 토큰 부분만 반환
            return bearerToken.substring(7);
        }
        
        return null;
    }

    /**
     * 특정 경로에 대해 필터를 건너뛸지 결정
     * OAuth2 관련 경로나 공개 API는 JWT 검증 건너뛰기
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // JWT 인증이 불필요한 경로들
        return path.startsWith("/oauth2/") ||           // OAuth2 관련
               path.startsWith("/login/") ||            // 로그인 관련  
               path.equals("/api/auth/status") ||       // 상태 확인
               path.equals("/api/auth/hello") ||        // 테스트 API
               path.equals("/api/auth/test-jwt") ||     // JWT 테스트
               path.startsWith("/api/test/") ||         // 기존 테스트 API
               path.startsWith("/api/download/") ||     // 파일 다운로드
               path.equals("/") ||                      // 루트
               path.equals("/error");                   // 에러 페이지
    }
}
