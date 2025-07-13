package com.plana.auth.config;

import com.plana.auth.repository.MemberRepository;
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
import com.plana.auth.dto.AuthenticatedMemberDto;

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
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {

        // 현재 요청에 대한 인증 정보 로깅 (디버그용)
        logAuthenticationInfo(request);

        try {
            // Authorization 헤더에서 JWT 토큰 추출
            String token = getTokenFromRequest(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 토큰에서 사용자 ID 추출
                Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

                // 사용자 정보 조회
                Optional<AuthenticatedMemberDto> memberOptional = memberRepository.findAuthenticatedMemberById(memberId);

                if (memberOptional.isPresent()) {
                    AuthenticatedMemberDto member = memberOptional.get();

                    // 계정이 탈퇴하지 않은 상태인지 확인
                    if (!member.isDeleted()) {
                        // 토큰에서 권한 정보 추출, 일반 사용자인지 확인 작업
                        String role = jwtTokenProvider.getRoleFromToken(token);
                        // 🔧 수정: 빈 문자열도 체크하고 ROLE_ 접두사 추가
                        if (role == null || role.trim().isEmpty()) {
                            role = "ROLE_USER"; // 기본 권한
                        } else if (!role.startsWith("ROLE_")) {
                            role = "ROLE_" + role; // ROLE_ 접두사 추가
                        }

                        // Spring Security 인증 객체 생성
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                member,  // Principal (인증된 사용자 정보)
                                null,  // Credentials (비밀번호 등, JWT에서는 불필요)
                                Collections.singletonList(new SimpleGrantedAuthority(role)) // 권한
                            );

                        // SecurityContext에 인증 정보 설정
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("JWT authentication successful for member.email: {} (member.id: {})",
                                member.getId(), member.getEmail());
                    } else {
                        log.warn("Deleted member account access attempt: {} (member.id: {})", member.getEmail(), member.getId());
                        clearSecurityContext();
                    }
                } else {
                    log.warn("Member not found for token member.id: {}", memberId);
                    clearSecurityContext();
                }

            } else if (token != null) {
                log.debug("Invalid JWT token received for request: {}", request.getRequestURI());
                clearSecurityContext();
            }
            // token이 null인 경우는 로그하지 않음 (공개 API 요청)

        } catch (Exception e) {
            log.error("JWT authentication failed for request: {} - Error: {}", request.getRequestURI(), e.getMessage());
            clearSecurityContext();
        }

        // 다음 필터 실행 (인증 실패 시에도 계속 진행)
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
     * SecurityContext 초기화
     * 인증 실패 시 기존 인증 정보 제거
     */
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 특정 경로에 대해 필터를 건너뛸지 결정
     * JWT 인증이 불필요한 공개 API 경로들
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 요청은 CORS preflight이므로 JWT 검증 건너뛰기
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // JWT 인증이 불필요한 경로들 (SecurityConfig의 permitAll과 일치)
        return path.equals("/") ||                              // 루트
                path.startsWith("/api/test/") ||                // 테스트 API (기존)
                path.startsWith("/api/download/") ||            // 파일 다운로드 API (기존)
                path.equals("/api/auth/status") ||              // 상태 확인 API
                path.equals("/api/auth/hello") ||               // 테스트 API
                path.equals("/api/auth/test-jwt") ||            // JWT 테스트 API

                // === 추가된 경로들 ===
                path.equals("/api/members") ||                  // POST: 회원가입, GET: 이메일 중복 확인.. 흠 signup 과 같이 endpoint 를 명시적으로 사용하는 것은 어떤지 의견나눠보기!
                path.equals("/api/auth/signup") ||              // 일반 회원가입 API
                path.equals("/api/auth/login") ||               // 일반 로그인 API
                path.equals("/api/auth/social-login") ||        // OAuth2 로그인 엔드포인트
                path.equals("/api/auth/social-signup") ||       // 소셜 회원가입
                path.equals("/api/auth/reset/verify") ||        // 인증 코드 확인
                path.equals("/api/auth/id-recovery") ||         // 아이디 찾기
                path.equals("/api/auth/password-reset/verify") || // 비밀번호 찾기

                path.equals("/error") ||                        // 에러 페이지
                path.startsWith("/v3/api-docs/") ||             // Swaager가 자동 생성하는 API 명세 JSON 데이터가 위치하는 기본 URL 경로
                path.startsWith("/swagger-ui/") ||              // Swagger UI관련 정적리소스가 위치하는 경로
                path.equals("/swagger-ui.html");                // Swagger UI를 열기 위한 메인 HTML 페이지 URL


    }

    /**
     * 현재 요청에 대한 인증 정보 로깅 (디버그용)
     * @param request HTTP 요청
     */
    private void logAuthenticationInfo(HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            String token = getTokenFromRequest(request);
            log.debug("=== JWT Filter Debug Info ===");
            log.debug("Request URI: {}", request.getRequestURI());
            log.debug("Request Method: {}", request.getMethod());
            log.debug("Token Present: {}", token != null);
            log.debug("Should Filter: {}", !shouldNotFilter(request));

            if (token != null) {
                try {
                    Long memCode = jwtTokenProvider.getMemberIdFromToken(token);
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    log.debug("Token MemCode: {}, Email: {}", memCode, email);
                } catch (Exception e) {
                    log.debug("Token parsing failed: {}", e.getMessage());
                }
            }
            log.debug("============================");
        }
    }
}
