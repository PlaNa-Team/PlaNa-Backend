package com.plana.notification.listener;

import com.plana.notification.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;

/**
 * WebSocket 이벤트 리스너
 *
 * WebSocket 연결/해제 및 구독/구독해제 이벤트를 처리하여
 * 사용자 세션을 관리하고 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    /**
     * WebSocket 연결 이벤트 처리
     *
     * @param event 연결 이벤트
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.debug("WebSocket 연결 이벤트: sessionId={}", sessionId);

        try {
            // 핸드셰이크 인터셉터에서 설정한 세션 속성에서 사용자 정보 추출
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

            if (sessionAttributes != null && sessionAttributes.containsKey("memberId")) {
                Long memberId = (Long) sessionAttributes.get("memberId");
                String memberEmail = (String) sessionAttributes.get("memberEmail");

                // 세션 매니저에 사용자 등록
                sessionManager.addUserSession(memberId, sessionId);
                log.info("WebSocket 연결 성공: memberId={}, email={}, sessionId={}",
                        memberId, memberEmail, sessionId);
            } else {
                log.warn("인증되지 않은 WebSocket 연결 시도: sessionId={} (핸드셰이크에서 차단되었어야 함)", sessionId);
            }

        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 해제 이벤트 처리
     *
     * @param event 연결 해제 이벤트
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 연결 해제됨: sessionId={}", sessionId);

        try {
            // 세션 관리자에서 해당 세션 제거
            Long memberId = sessionManager.getMemberIdBySession(sessionId);
            if (memberId != null) {
                sessionManager.removeSession(sessionId);
                log.info("사용자 WebSocket 연결 해제: memberId={}, sessionId={}", memberId, sessionId);
            } else {
                log.debug("세션 관리자에 등록되지 않은 연결 해제: sessionId={}", sessionId);
            }

        } catch (Exception e) {
            log.error("WebSocket 연결 해제 처리 중 오류 발생: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * WebSocket 구독 이벤트 처리
     *
     * @param event 구독 이벤트
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.debug("WebSocket 구독: sessionId={}, destination={}", sessionId, destination);

        try {
            // 개인 알림 채널 구독 확인
            if (destination != null && destination.startsWith("/user/")) {
                Long memberId = sessionManager.getMemberIdBySession(sessionId);
                if (memberId != null) {
                    // /user/queue/notifications 형태의 구독인지 확인
                    if (destination.equals("/user/queue/notifications")) {
                        log.info("개인 알림 채널 구독 성공: memberId={}, destination={}", memberId, destination);
                    } else {
                        log.warn("알 수 없는 개인 채널 구독 시도: memberId={}, destination={}", memberId, destination);
                    }
                } else {
                    log.warn("인증되지 않은 사용자의 개인 채널 구독 시도: sessionId={}, destination={}",
                            sessionId, destination);
                }
            }

        } catch (Exception e) {
            log.error("WebSocket 구독 처리 중 오류 발생: sessionId={}, destination={}, error={}",
                    sessionId, destination, e.getMessage(), e);
        }
    }

    /**
     * WebSocket 구독 해제 이벤트 처리
     *
     * @param event 구독 해제 이벤트
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.debug("WebSocket 구독 해제: sessionId={}", sessionId);
    }
}