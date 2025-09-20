package com.plana.notification.util;

import com.plana.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket 인증 유틸리티
 *
 * WebSocket 연결 시 JWT 토큰을 통한 사용자 인증 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthUtil {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * WebSocket 메시지 헤더에서 JWT 토큰을 추출하고 사용자 ID 반환
     *
     * @param headerAccessor WebSocket 메시지 헤더
     * @return 사용자 ID (인증 실패 시 null)
     */
    public Long extractMemberIdFromHeaders(SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 1. Authorization 헤더에서 토큰 추출
            String token = extractTokenFromHeaders(headerAccessor);
            if (token == null) {
                log.warn("WebSocket 연결에서 JWT 토큰을 찾을 수 없습니다.");
                return null;
            }

            // 2. 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("WebSocket 연결에서 유효하지 않은 JWT 토큰입니다.");
                return null;
            }

            // 3. 토큰에서 사용자 ID 추출
            Long memberId = jwtTokenProvider.getMemberIdFromToken(token);
            log.debug("WebSocket 인증 성공: memberId={}", memberId);
            return memberId;

        } catch (Exception e) {
            log.error("WebSocket 인증 처리 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * WebSocket 헤더에서 JWT 토큰 추출
     *
     * @param headerAccessor 헤더 접근자
     * @return JWT 토큰 (Bearer 접두사 제거된 순수 토큰, 없으면 null)
     */
    private String extractTokenFromHeaders(SimpMessageHeaderAccessor headerAccessor) {
        // 방법 1: STOMP 명령 헤더에서 Authorization 찾기
        List<String> authHeaders = headerAccessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7); // "Bearer " 제거
            }
        }

        // 방법 2: 연결 시 전달된 쿼리 파라미터에서 토큰 찾기
        // 예: /ws?token=jwt_token_here
        Object tokenParam = headerAccessor.getSessionAttributes().get("token");
        if (tokenParam instanceof String) {
            return (String) tokenParam;
        }

        // 방법 3: User Principal에서 토큰 추출 (STOMP 인터셉터에서 설정된 경우)
        Object userPrincipal = headerAccessor.getSessionAttributes().get("userToken");
        if (userPrincipal instanceof String) {
            return (String) userPrincipal;
        }

        return null;
    }

    /**
     * 인증 성공 여부 확인
     *
     * @param headerAccessor 헤더 접근자
     * @return 인증 성공 여부
     */
    public boolean isAuthenticated(SimpMessageHeaderAccessor headerAccessor) {
        return extractMemberIdFromHeaders(headerAccessor) != null;
    }
}