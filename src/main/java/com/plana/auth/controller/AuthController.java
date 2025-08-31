package com.plana.auth.controller;

import com.plana.auth.dto.*;
import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.auth.service.EmailVerificationService;
import com.plana.auth.service.JwtTokenProvider;
import com.plana.auth.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 인증 관련 API 컨트롤러
 * OAuth2 로그인 테스트, 토큰 갱신, 로그아웃, 사용자 정보 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityMs;

    @Value("${jwt.refresh-token-validity-remember}")
    private long refreshTokenValidityRememberMs;

    @Value("${jwt.refresh-token-validity-short}")   // 예: 60초 (테스트)
    private long refreshTokenValidityShortMs;

    @Value("${jwt.refresh-token-validity-long}")   // 예: 120초 (테스트)
    private long refreshTokenValidityLongMs;
    

    /**
     * 현재 로그인한 사용자 정보 조회 (JWT 기반)
     * 실제 프론트엔드에서 사용할 회원정보 API
     //* @param principal Spring Security에서 인증된 사용자 정보
     * @return 사용자 정보 응답
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        if (authMember == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        // DB에서 최신 사용자 정보 조회 (토큰의 정보가 오래될 수 있음)
        Optional<Member> memberOptional = memberRepository.findById(authMember.getId());
        
        if (memberOptional.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        Member member = memberOptional.get();
        
        // 계정 비활성화 상태 확인
        if (!member.getEnabled()) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is disabled"));
        }
        
        // 깔끔한 사용자 정보 응답
        Map<String, Object> response = new HashMap<>();
        response.put("id", member.getId());
        response.put("email", member.getEmail());
        response.put("name", member.getName());
        response.put("profileImageUrl", member.getProfileImageUrl());
        response.put("provider", member.getProvider().getValue());
        response.put("role", member.getRole());
        response.put("enabled", member.getEnabled());
        response.put("createdAt", member.getCreatedAt());
        response.put("updatedAt", member.getUpdatedAt());
        
        log.info("User info requested via JWT: {}", member.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * 인증된 사용자 정보 조회 (OAuth2 로그인 확인용)
     * @param oAuth2User OAuth2 인증된 사용자 정보
     * @return 사용자 정보 응답
     */
    @GetMapping("/member")
    public ResponseEntity<Map<String, Object>> getAuthenticatedUser(
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        
        if (oAuth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("name", oAuth2User.getName());
        response.put("attributes", oAuth2User.getAttributes());
        response.put("authorities", oAuth2User.getAuthorities());
        
        log.info("Authenticated member info requested: {}", oAuth2User.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신 API
     * 리프레시 토큰으로 새로운 액세스 토큰 발급
     * @param request HTTP 요청 (쿠키에서 리프레시 토큰 추출)
     * @return 새로운 액세스 토큰
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(HttpServletRequest request, HttpServletResponse rp) {
        
        // 쿠키에서 리프레시 토큰 추출
        String refreshToken = getCookieValue(request, "refreshToken");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token not found"));
        }

        try {
            // 리프레시 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
            }

            // 리프레시 토큰에서 사용자 ID 추출
            Long memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken);
            
            // 사용자 정보 조회
            Optional<Member> memberOptional = memberRepository.findById(memberId);
            if (memberOptional.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            Member member = memberOptional.get();
            
            // 새로운 액세스 토큰 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole()
            );

            // Refresh 회전 + 슬라이딩 연장
            // 임박 기준 : 3일 이내면 true
            boolean aboutToExpire = jwtTokenProvider.isAboutToExpire(refreshToken, Duration.ofDays(3));

            long remainingSec = jwtTokenProvider.getRemainingSeconds(refreshToken);
            boolean longLived = remainingSec > (refreshTokenValidityShortMs / 1000); // 짧은 TTL 초과면 장기 취급
            long nextRtMs = longLived ? refreshTokenValidityLongMs : refreshTokenValidityShortMs;

            String nextRefresh = jwtTokenProvider.createRefreshToken(member.getId(), nextRtMs);


            ResponseCookie rtCookie = ResponseCookie.from("refreshToken", nextRefresh)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("None")
                    .maxAge(nextRtMs / 1000)
                    .build();
            rp.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

            Map<String, Object> response = new HashMap<>();
            long expiresIn = Math.max(0,
                    (jwtTokenProvider.getExpirationDateFromToken(newAccessToken).getTime() - System.currentTimeMillis()) / 1000);

            response.put("accessToken", newAccessToken);
            response.put("message", "Token refreshed successfully");
            response.put("expiresIn", expiresIn);
            response.put("rotated", true);
            response.put("slidingExtended", aboutToExpire);
            
            log.info("Token refreshed for member: {}", member.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(401).body(Map.of("error", "Token refresh failed"));
        }
    }

    /**
     * 로그아웃 API
     * 리프레시 토큰 쿠키를 삭제하여 로그아웃 처리
     * @param request HTTP 요청
     * @param response HTTP 응답  
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, 
                                                      HttpServletResponse response) {
        
        // 기존 리프레시 토큰 확인
        String refreshToken = getCookieValue(request, "refreshToken");
        
        if (refreshToken != null) {
            log.info("Logout requested with refresh token present");
        } else {
            log.info("Logout requested without refresh token");
        }
        
        // 리프레시 토큰 쿠키 삭제
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 개발 환경 (프로덕션에서는 true)
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Logout successful");
        responseBody.put("timestamp", System.currentTimeMillis());
        responseBody.put("info", "Refresh token cookie has been cleared");
        
        log.info("User logged out successfully");
        return ResponseEntity.ok(responseBody);
    }

    /**
     * OAuth2 로그인 상태 확인
     * @return 로그인 상태 응답
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> response = new HashMap<>();
                response.put("message", "Authentication server is running");
        response.put("timestamp", System.currentTimeMillis());
        response.put("oauth2_enabled", true);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 소셜 로그인 테스트용 헬로 메시지
     * @return 테스트 메시지
     */
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Auth Controller!");
        response.put("info", "OAuth2 social login is configured");
        
        return ResponseEntity.ok(response);
    }

    /**
     * JWT 토큰 테스트 (개발/테스트용)
     * 실제 프로덕션에서는 제거해야 할 API
     */
    @GetMapping("/test-jwt")
    public ResponseEntity<Map<String, Object>> testJwt() {
        // 테스트용 JWT 토큰 생성
        String testToken = jwtTokenProvider.createAccessToken(1L, "test@example.com", "ROLE_USER");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "JWT test token generated");
        response.put("token", testToken);
        response.put("warning", "This is for testing only - remove in production");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 일반 회원가입 API
     * Handle 이메일, 비밀번호, 이름으로 회원가입 처리
     * @param signupRequest 회원가입 요청 정보
     * @return 회원가입 결과
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto signupRequest) {
        log.info("General signup API called: {}", signupRequest.getEmail());

        SignupResponseDto response = memberService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 일반 로그인 API
     * Handle 이메일과 비밀번호로 로그인 처리
     * @param loginRequest 로그인 요청 정보
     * @return 로그인 결과 (JWT 토큰 포함)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest, HttpServletResponse httpResponse) {
        log.info("General login API called: {}", loginRequest.getEmail());

        IssuedTokens response = memberService.login(loginRequest);

        // HttpOnly Refresh 쿠키 세팅 (웹 계층 책임)
        ResponseCookie rtCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)       // 운영은 HTTPS 필수
                .path("/")
                .sameSite("None")
                .maxAge(response.getRefreshMaxAgeSec())
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

        // 바디에는 Access만
        return ResponseEntity.ok(
                LoginResponseDto.builder()
                        .accessToken(response.getAccessToken())
                        .expiresIn(response.getAccessExpiresInSec())
                        .member(response.getMember())
                        .build()
        );
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

    // 이메일 인증번호 발송
    @PostMapping("/email/verification-code")
    public ResponseEntity<EmailSendResponseDto> sendVerificationCode(@Valid @RequestBody EmailSendRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        boolean duplicated = emailVerificationService.sendCodeIfNotDuplicated(email);
        return duplicated
                ? ResponseEntity.status(409).body(EmailSendResponseDto.duplicated())
                : ResponseEntity.ok(EmailSendResponseDto.sent());
    }

    // 이메일 인증번호 확인
    @PostMapping("/email/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyCodeRequestDto req) {
        var r = emailVerificationService.verifyCode(req.getEmail(), req.getCode());
        return switch (r) {
            case OK -> ResponseEntity.ok(Map.of("status", 200, "verified", true, "message", "이메일 인증이 완료되었습니다."));
            case MISMATCH -> ResponseEntity.badRequest().body(Map.of("status", 400, "verified", false, "message", "인증번호가 일치하지 않습니다."));
            case EXPIRED, NOT_FOUND -> ResponseEntity.status(410).body(Map.of("status", 410, "verified", false, "message", "인증번호가 만료되었거나 존재하지 않습니다."));
        };
    }

}
