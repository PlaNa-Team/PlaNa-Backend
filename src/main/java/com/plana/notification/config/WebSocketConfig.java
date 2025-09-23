package com.plana.notification.config;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    /**
     * STOMP 엔드포인트 등록
     * 클라이언트가 WebSocket 연결을 위해 접속할 URL 설정
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 순수 WebSocket 엔드포인트 (실제 사용되는 엔드포인트 설정SockJS 없이)
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns(
//                         "*",                         // 개발 테스트용
                        "http://localhost:3000",        // React 기본 포트
                        "http://localhost:5173",        // Vite 기본 포트
                        "http://localhost:5174",        // Vite 대체 포트
                        "http://localhost:80",          // HTTP
                        "http://localhost",             // 포트 없는 localhost
                        "http://localhost:443",         // HTTPS 포트
                        "https://localhost:443",        // HTTPS
                        "http://hoonee-math.info",      // 기존 프로덕션 도메인 (HTTP)
                        "https://hoonee-math.info",     // 기존 프로덕션 도메인 (HTTPS)
                        "http://plana.hoonee-math.info",   // 플래너 프로덕션 도메인 (HTTP)
                        "https://plana.hoonee-math.info",  // 플래너 프로덕션 도메인 (HTTPS)
                        "http://plana-frontend-silk.vercel.app",
                        "https://plana-frontend-silk.vercel.app"
                )
                .addInterceptors(jwtHandshakeInterceptor);  // JWT 인증 인터셉터 추가

        // SockJS 폴백 엔드포인트 (별도, 25-09-23T19:50 기준 사용하지 않는 코드, WebSocket 미지원 브라우저 대응이라지만 사용하지 않아 삭제할 예정임)
        registry.addEndpoint("/api/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS()
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);
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