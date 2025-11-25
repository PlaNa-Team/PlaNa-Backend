package com.plana.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket 연결 테스트를 위한 서비스
 * 주기적으로 테스트 메시지를 발송하여 연결 상태를 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketTestService {
/*
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
*/
    /**
     * 10초마다 온라인 사용자들에게 테스트 메시지 발송
     *
         [주석 처리 사유]
         - 프로덕션 환경에서 불필요한 자동 테스트 메시지 발송 방지
         - 10초마다 모든 온라인 사용자에게 메시지를 보내어 불필요한 트래픽 발생
         - 실제 알림이 아닌 테스트 메시지로 사용자 경험 저하
         - 수동 테스트가 필요한 경우 NotificationController.sendTestMessage() API 사용 권장
         - 개발 환경에서만 활성화하려면 @Profile("dev") 어노테이션 추가 고려
     */
    /*
    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    public void sendPeriodicTestMessage() {
        try {
            // 온라인 사용자 목록 조회
            Set<Long> onlineUsers = sessionManager.getOnlineUsers();

            if (onlineUsers.isEmpty()) {
                log.debug("온라인 사용자가 없어서 테스트 메시지를 발송하지 않습니다.");
                return;
            }
            System.out.println("onlineUsers: " + onlineUsers);

            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // 각 온라인 사용자에게 테스트 메시지 발송
            for (Long memberId : onlineUsers) {
                Map<String, Object> testMessage = new HashMap<>();
                testMessage.put("type", "TEST");
                testMessage.put("message", "WebSocket 연결 테스트 메시지");
                testMessage.put("time", currentTime);
                testMessage.put("memberId", memberId);

                // 직접 경로로 발송 (확인된 작동 방식)
                String directDestination = "/user/" + memberId + "/queue/notifications";
                messagingTemplate.convertAndSend(directDestination, testMessage);

                log.info("테스트 메시지 발송: memberId={}, time={}", memberId, currentTime);
            }

        } catch (Exception e) {
            log.error("테스트 메시지 발송 중 오류 발생: {}", e.getMessage(), e);
        }
    }
*/
}