package com.plana.auth.config;

import com.plana.auth.entity.Member;
import com.plana.auth.service.JwtTokenProvider;
import com.plana.auth.service.OAuth2UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 소셜 로그인 성공 시 처리하는 핸들러
 * JWT 토큰을 생성하고 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    
    // 프론트엔드 리다이렉트 URL (application-private.properties에서 설정)
    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:5173/auth/callback}")
    private String authorizedRedirectUri;

    /**
     * OAuth2 로그인 성공 시 실행되는 메서드
     * JWT 토큰을 생성하고 프론트엔드로 리다이렉트
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        // OAuth2UserService에서 생성한 CustomOAuth2User에서 User 정보 추출
        OAuth2UserService.CustomOAuth2User oAuth2User = 
            (OAuth2UserService.CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getUser();
        
        log.info("OAuth2 login success for member: {}", member.getEmail());

        try {
            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(
                    member.getId(),
                    member.getEmail(),
                    member.getRole()
            );
            String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
            
            // 리프레시 토큰을 HttpOnly 쿠키로 설정 (보안상 안전)
            addRefreshTokenCookie(response, refreshToken);
            
            // 프론트엔드로 리다이렉트 URL 생성 (액세스 토큰 포함)
            String targetUrl = createTargetUrl(accessToken, member);
            
            log.info("Redirecting to: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            
        } catch (Exception e) {
            log.error("Error during OAuth2 success handling", e);
            
            // 에러 발생 시 에러 페이지로 리다이렉트
            String errorUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                    .queryParam("error", "token_generation_failed")
                    .build().toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    /**
     * 프론트엔드 리다이렉트 URL 생성
     * @param accessToken JWT 액세스 토큰
     * @param member 사용자 정보
     * @return 리다이렉트 URL
     */
    private String createTargetUrl(String accessToken, Member member) {
        return UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .queryParam("token", accessToken) // 액세스 토큰
                .queryParam("memberId", member.getId()) // 사용자 ID
                .queryParam("email", URLEncoder.encode(member.getEmail(), StandardCharsets.UTF_8)) // 이메일 (URL 인코딩)
                .queryParam("name", URLEncoder.encode(member.getName(), StandardCharsets.UTF_8)) // 이름 (URL 인코딩)
                .queryParam("provider", member.getProvider().getValue()) // 소셜 제공업체
                .build().toUriString();
    }

    /**
     * 리프레시 토큰을 HttpOnly 쿠키로 설정
     * @param response HTTP 응답
     * @param refreshToken 리프레시 토큰
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가 (XSS 방지)
        cookie.setSecure(false); // 개발 환경에서는 false, 프로덕션에서는 true로 설정
        cookie.setPath("/"); // 모든 경로에서 접근 가능
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일간 유효
        
        response.addCookie(cookie);
        log.debug("Refresh token cookie added");
    }

    /**
     * 쿠키에서 특정 이름의 값을 가져오는 유틸리티 메서드
     * @param request HTTP 요청
     * @param name 쿠키 이름
     * @return 쿠키 값 (없으면 null)
     */
    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
