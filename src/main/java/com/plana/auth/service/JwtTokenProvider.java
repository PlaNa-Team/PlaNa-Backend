package com.plana.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 서비스
 * 액세스 토큰과 리프레시 토큰을 모두 처리
 */
@Slf4j
@Component
public class JwtTokenProvider {

    // JWT 서명에 사용할 비밀키 (application.properties에서 주입)
    private final SecretKey secretKey;
    
    // 액세스 토큰 만료시간 (기본: 1시간)
    private final long accessTokenValidityInMilliseconds;
    
    // 리프레시 토큰 만료시간 (기본: 7일)
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity:3600000}") long accessTokenValidityInMilliseconds,
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityInMilliseconds) {
        
        // 비밀키 유효성 검증 (반드시 application.properties에 설정 필요)
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret key must be provided in application.properties");
        }
        
        if (secretKey.length() < 64) {
            throw new IllegalArgumentException("JWT secret key must be at least 64 characters long");
        }
        
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
    }

    /**
     * 액세스 토큰 생성
     * @param memberId 사용자 ID
     * @param email 사용자 이메일
     * @param role 사용자 권한
     * @return JWT 액세스 토큰
     */
    public String createAccessToken(Long memberId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(memberId.toString()) // 토큰 주체 (사용자 ID)
                .claim("email", email) // 사용자 이메일
                .claim("role", role) // 사용자 권한
                .claim("type", "access") // 토큰 타입
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiryDate) // 만료 시간
                .signWith(secretKey, SignatureAlgorithm.HS512) // 서명
                .compact();
    }

    /**
     * 리프레시 토큰 생성
     * @param memberId 사용자 ID
     * @return JWT 리프레시 토큰
     */
    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(memberId.toString()) // 토큰 주체 (사용자 ID)
                .claim("type", "refresh") // 토큰 타입
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiryDate) // 만료 시간
                .signWith(secretKey, SignatureAlgorithm.HS512) // 서명
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getMemberIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰에서 이메일 추출
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * 토큰에서 권한 추출
     * @param token JWT 토큰
     * @return 사용자 권한
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 Claims 추출
     * @param token JWT 토큰
     * @return Claims 객체
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰 만료 시간 확인
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 토큰이 만료되었는지 확인
     * @param token JWT 토큰
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}
