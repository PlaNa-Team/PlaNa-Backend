package com.plana.notification.config;

import com.plana.auth.repository.MemberRepository;
import com.plana.auth.service.JwtTokenProvider;
import com.plana.auth.dto.AuthenticatedMemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WebSocket 핸드셰이크 시 JWT 토큰 인증을 처리하는 인터셉터
 *
 * 연결 단계에서 JWT 토큰을 검증하고, 인증된 사용자 정보를 WebSocket 세션에 저장
 * 인증 실패 시 WebSocket 연결을 거부함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    /**
     * WebSocket 핸드셰이크 전에 JWT 토큰을 검증
     *
     * @param request    HTTP 요청
     * @param response   HTTP 응답
     * @param wsHandler  WebSocket 핸들러
     * @param attributes WebSocket 세션 속성 (인증된 사용자 정보 저장용)
     * @return 인증 성공 시 true (연결 허용), 실패 시 false (연결 거부)
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        log.debug("WebSocket 핸드셰이크 시작: {}", request.getURI());

        try {
            // JWT 토큰 추출
            String token = extractTokenFromRequest(request);

            if (token == null) {
                log.warn("WebSocket 연결 시 JWT 토큰이 없습니다: {}", request.getURI());
                return false; // 연결 거부
            }

            // 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("WebSocket 연결 시 유효하지 않은 JWT 토큰: {}", request.getURI());
                return false; // 연결 거부
            }

            // 토큰에서 사용자 ID 추출
            Long memberId = jwtTokenProvider.getMemberIdFromToken(token);
            log.debug("WebSocket 연결 요청 사용자 ID: {}", memberId);

            // 사용자 정보 조회
            Optional<AuthenticatedMemberDto> memberOptional = memberRepository.findAuthenticatedMemberById(memberId);

            if (memberOptional.isEmpty()) {
                log.warn("WebSocket 연결 시 사용자를 찾을 수 없음: memberId={}", memberId);
                return false; // 연결 거부
            }

            AuthenticatedMemberDto member = memberOptional.get();

            // 탈퇴한 사용자 확인
            if (member.isDeleted()) {
                log.warn("WebSocket 연결 시 탈퇴한 사용자: memberId={}, email={}", memberId, member.getEmail());
                return false; // 연결 거부
            }

            // 인증된 사용자 정보를 WebSocket 세션 속성에 저장
            attributes.put("memberId", memberId);
            attributes.put("memberEmail", member.getEmail());
            attributes.put("memberName", member.getName());
            attributes.put("authenticatedMember", member);

            log.info("WebSocket 핸드셰이크 인증 성공: memberId={}, email={}", memberId, member.getEmail());
            return true; // 연결 허용

        } catch (Exception e) {
            log.error("WebSocket 핸드셰이크 중 오류 발생: {}", e.getMessage(), e);
            return false; // 연결 거부
        }
    }

    /**
     * WebSocket 핸드셰이크 완료 후 호출 (현재는 특별한 처리 없음)
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.error("WebSocket 핸드셰이크 완료 후 오류: {}", exception.getMessage(), exception);
        } else {
            log.debug("WebSocket 핸드셰이크 완료: {}", request.getURI());
        }
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     *
     * 우선순위:
     * 1. Authorization 헤더 (Bearer 토큰)
     * 2. 쿼리 파라미터 (token, access_token)
     *
     * @param request HTTP 요청
     * @return JWT 토큰 (순수 토큰, Bearer 접두사 제거됨), 없으면 null
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // 1. Authorization 헤더에서 토큰 추출
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7); // "Bearer " 제거
                log.debug("Authorization 헤더에서 JWT 토큰 추출: 길이={}", token.length());
                return token;
            }
        }

        // 2. 쿼리 파라미터에서 토큰 추출
        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null) {
            // ?token=xxx 또는 ?access_token=xxx 형태
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];

                    if ("token".equals(key) || "access_token".equals(key)) {
                        log.debug("쿼리 파라미터에서 JWT 토큰 추출: key={}, 길이={}", key, value.length());
                        return value;
                    }
                }
            }
        }

        log.debug("JWT 토큰을 찾을 수 없음: {}", request.getURI());
        return null;
    }
}