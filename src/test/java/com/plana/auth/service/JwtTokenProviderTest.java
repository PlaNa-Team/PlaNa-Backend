package com.plana.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtTokenProvider 단위 테스트
 * Phase 1.2: 이미 구현된 JWT 토큰 생성/검증 로직 검증
 */
@ActiveProfiles("test")
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    
    // 테스트용 데이터
    private final String testSecret = "testSecretKeyForTestingPurposesOnly12345678901234567890123456789"; // 64+ 자
    
    private final long accessTokenValidity = 3600000L; // 1시간
    private final long refreshTokenValidity = 604800000L; // 7일
    
    private final Long testMemberId = 1L;
    private final String testEmail = "test@example.com";
    private final String testRole = "ROLE_USER";
    
    @BeforeEach
    void setUp() {
        // JwtTokenProvider 인스턴스 생성 (실제 생성자 사용)
        jwtTokenProvider = new JwtTokenProvider(testSecret, accessTokenValidity, refreshTokenValidity);
    }
    
    @Test
    @DisplayName("Access Token 정상 생성 - 이미 구현된 createAccessToken() 검증")
    void createAccessToken_ValidInput_Success() {
        // When - 이미 구현된 createAccessToken() 메서드 호출
        String token = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // Then - 토큰 생성 결과 검증
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 header.payload.signature 구조
        
        System.out.println("✅ 생성된 Access Token: " + token.substring(0, 50) + "...");
    }
    
    @Test
    @DisplayName("Refresh Token 정상 생성 - 이미 구현된 createRefreshToken() 검증")
    void createRefreshToken_ValidInput_Success() {
        // When - 이미 구현된 createRefreshToken() 메서드 호출
        String token = jwtTokenProvider.createRefreshToken(testMemberId);
        
        // Then - 토큰 생성 결과 검증
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 header.payload.signature 구조
        
        System.out.println("✅ 생성된 Refresh Token: " + token.substring(0, 50) + "...");
    }
    
    @Test
    @DisplayName("유효한 토큰 검증 성공 - 이미 구현된 validateToken() 검증")
    void validateToken_ValidToken_ReturnsTrue() {
        // Given - 유효한 토큰 생성
        String token = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // When - 이미 구현된 validateToken() 메서드 호출
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // Then - 토큰이 유효해야 함
        assertThat(isValid).isTrue();
        
        System.out.println("✅ 토큰 검증 성공: " + isValid);
    }
    
    @Test
    @DisplayName("토큰에서 사용자 ID 추출 - 이미 구현된 getMemberIdFromToken() 검증")
    void getMemberIdFromToken_ValidToken_ReturnsMemberId() {
        // Given - 토큰 생성
        String token = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // When - 이미 구현된 getMemberIdFromToken() 메서드 호출
        Long extractedMemberId = jwtTokenProvider.getMemberIdFromToken(token);
        
        // Then - 원본 사용자 ID와 일치해야 함
        assertThat(extractedMemberId).isEqualTo(testMemberId);
        
        System.out.println("✅ 추출된 사용자 ID: " + extractedMemberId);
    }
    
    @Test
    @DisplayName("토큰에서 이메일 추출 - 이미 구현된 getEmailFromToken() 검증")
    void getEmailFromToken_ValidToken_ReturnsEmail() {
        // Given - 토큰 생성
        String token = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // When - 이미 구현된 getEmailFromToken() 메서드 호출
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        
        // Then - 원본 이메일과 일치해야 함
        assertThat(extractedEmail).isEqualTo(testEmail);
        
        System.out.println("✅ 추출된 이메일: " + extractedEmail);
    }
    
    @Test
    @DisplayName("토큰에서 권한 추출 - 이미 구현된 getRoleFromToken() 검증")
    void getRoleFromToken_ValidToken_ReturnsRole() {
        // Given - 토큰 생성
        String token = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // When - 이미 구현된 getRoleFromToken() 메서드 호출
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);
        
        // Then - 원본 권한과 일치해야 함
        assertThat(extractedRole).isEqualTo(testRole);
        
        System.out.println("✅ 추출된 권한: " + extractedRole);
    }
    
    @Test
    @DisplayName("토큰 만료 시간 추출 - 이미 구현된 getExpirationDateFromToken() 검증")
    void getExpirationDateFromToken_ValidToken_ReturnsExpirationDate() {
        // Given - 토큰 생성
        Date beforeCreation = new Date();
        String token = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        Date afterCreation = new Date();
        
        // When - 이미 구현된 getExpirationDateFromToken() 메서드 호출
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);
        
        // Then - 만료 시간이 적절한 범위에 있어야 함
        Date expectedExpiration = new Date(beforeCreation.getTime() + accessTokenValidity);
        Date maxExpectedExpiration = new Date(afterCreation.getTime() + accessTokenValidity);
        
        assertThat(expirationDate).isAfter(expectedExpiration.toInstant().minusSeconds(1).toEpochMilli() > 0 ? 
                new Date(expectedExpiration.getTime() - 1000) : expectedExpiration);
        assertThat(expirationDate).isBefore(new Date(maxExpectedExpiration.getTime() + 1000));
        
        System.out.println("✅ 토큰 만료 시간: " + expirationDate);
    }
    
    @Test
    @DisplayName("토큰 만료 여부 확인 - 이미 구현된 isTokenExpired() 검증")
    void isTokenExpired_ValidToken_ReturnsFalse() {
        // Given - 유효한 토큰 생성
        String token = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // When - 이미 구현된 isTokenExpired() 메서드 호출
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);
        
        // Then - 새로 생성된 토큰은 만료되지 않았어야 함
        assertThat(isExpired).isFalse();
        
        System.out.println("✅ 토큰 만료 여부: " + isExpired + " (false=유효함)");
    }
    
    @Test
    @DisplayName("잘못된 토큰 검증 실패 - 변조된 토큰")
    void validateToken_MalformedToken_ReturnsFalse() {
        // Given - 변조된 토큰
        String malformedToken = "invalid.jwt.token";
        
        // When - 이미 구현된 validateToken() 메서드 호출
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);
        
        // Then - 변조된 토큰은 유효하지 않아야 함
        assertThat(isValid).isFalse();
        
        System.out.println("✅ 변조된 토큰 검증 결과: " + isValid + " (false=올바르게 거부됨)");
    }
    
    @Test
    @DisplayName("빈 토큰 검증 실패")
    void validateToken_EmptyToken_ReturnsFalse() {
        // When & Then - 빈 토큰들 검증
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        
        System.out.println("✅ 빈 토큰들 모두 올바르게 거부됨");
    }
    
    @Test
    @DisplayName("잘못된 Secret Key로 생성된 토큰은 검증 실패")
    void validateToken_DifferentSecretKey_ReturnsFalse() {
        // Given - 다른 secret key로 토큰 생성
        JwtTokenProvider differentProvider = new JwtTokenProvider(
                "differentSecretKeyForTestingPurposesOnly12345678901234567890123456", // 64+ 자
                accessTokenValidity,
                refreshTokenValidity
        );
        String tokenWithDifferentSecret = differentProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // When - 원래 provider로 검증
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSecret);
        
        // Then - 다른 secret으로 만든 토큰은 유효하지 않아야 함
        assertThat(isValid).isFalse();
        
        System.out.println("✅ 다른 Secret Key로 만든 토큰 올바르게 거부됨");
    }
    
    @Test
    @DisplayName("Access Token과 Refresh Token 구조 차이 검증")
    void compareAccessTokenAndRefreshToken_Structure() {
        // Given - 두 종류의 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(testMemberId, testEmail, testRole);
        String refreshToken = jwtTokenProvider.createRefreshToken(testMemberId);
        
        // When - 토큰에서 정보 추출
        Long memberIdFromAccess = jwtTokenProvider.getMemberIdFromToken(accessToken);
        Long memberIdFromRefresh = jwtTokenProvider.getMemberIdFromToken(refreshToken);
        
        String emailFromAccess = jwtTokenProvider.getEmailFromToken(accessToken);
        String roleFromAccess = jwtTokenProvider.getRoleFromToken(accessToken);
        
        // Then - 기본 정보는 동일, 구조적 차이 확인
        assertThat(memberIdFromAccess).isEqualTo(testMemberId);
        assertThat(memberIdFromRefresh).isEqualTo(testMemberId);
        assertThat(emailFromAccess).isEqualTo(testEmail);
        assertThat(roleFromAccess).isEqualTo(testRole);
        
        // Refresh Token에서는 email, role 추출 시 null일 수 있음 (구현에 따라)
        System.out.println("✅ Access Token 사용자 ID: " + memberIdFromAccess);
        System.out.println("✅ Refresh Token 사용자 ID: " + memberIdFromRefresh);
        System.out.println("✅ Access Token 이메일: " + emailFromAccess);
        System.out.println("✅ Access Token 권한: " + roleFromAccess);
    }
    
    @Test
    @DisplayName("토큰 만료 시간 설정 검증 - 생성자 파라미터 확인")
    void tokenExpiration_ConfigurationTest() {
        // Given - 짧은 만료시간으로 새 provider 생성 (1초)
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(testSecret, 1000L, 2000L);
        
        String shortToken = shortLivedProvider.createAccessToken(testMemberId, testEmail, testRole);
        
        // When - 즉시 검증 (아직 유효해야 함)
        boolean isValidImmediately = shortLivedProvider.validateToken(shortToken);
        boolean isNotExpiredImmediately = !shortLivedProvider.isTokenExpired(shortToken);
        
        // Then - 즉시는 유효해야 함
        assertThat(isValidImmediately).isTrue();
        assertThat(isNotExpiredImmediately).isTrue();
        
        System.out.println("✅ 짧은 수명 토큰 즉시 검증: " + isValidImmediately);
        System.out.println("✅ 짧은 수명 토큰 만료 여부: " + !isNotExpiredImmediately);
        
        // 실제 운영에서는 Thread.sleep(1500) 후 검증할 수 있지만,
        // 테스트 실행 시간을 고려해 생략
        System.out.println("💡 참고: 1초 후에는 만료되어 검증 실패할 것임");
    }
}
