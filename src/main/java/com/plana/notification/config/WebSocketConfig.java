package com.plana.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정 클래스
 *
 * STOMP(Simple Text Oriented Messaging Protocol) 기반 WebSocket 메시징 설정
 * - 실시간 알림 발송을 위한 양방향 통신 지원
 * - 개인별 알림 구독 및 브로드캐스트 메시징 지원
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * STOMP 엔드포인트 등록
     * 클라이언트가 WebSocket 연결을 위해 접속할 URL 설정
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS 설정 - 모든 Origin 허용 (개발용)
                .withSockJS();  // SockJS 폴백 지원 (WebSocket 미지원 브라우저 대응)
    }

    /**
     * 메시지 브로커 설정
     * 메시지 라우팅 및 구독 관리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트 → 서버 메시지 접두사
        // 예: /app/connect, /app/notifications
        config.setApplicationDestinationPrefixes("/app");

        // 서버 → 클라이언트 메시지 접두사
        // /topic: 브로드캐스트 메시징 (모든 구독자에게 전송)
        // /user: 개인별 메시징 (특정 사용자에게만 전송)
        config.enableSimpleBroker("/topic", "/user");

        // 개인별 메시지 접두사 설정
        // 예: /user/123/notifications (사용자 ID 123의 개인 알림)
        config.setUserDestinationPrefix("/user");
    }
}