package com.plana.notification.scheduler;

import com.plana.notification.entity.Notification;
import com.plana.notification.repository.NotificationRepository;
import com.plana.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 스케줄러
 *
 * 예정된 스케줄 알림을 실시간으로 발송하는 역할
 * - 1분마다 실행되어 발송 시간이 된 알림들을 처리
 * - 스케줄 알림(ALARM 타입)이고 isSent = false인 알림만 대상
 * - 발송 후 isRead는 사용자가 확인할 때까지 false 유지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * 예정된 스케줄 알림 처리
     *
     * 매 1분마다 실행되어 현재 시간이 된 알림들을 찾아서 발송
     * - time 필드가 현재 시간 이전이고 isSent = false인 ALARM 타입 알림들 처리
     * - 발송 후 isSent = true, sentAt = 현재시간으로 업데이트
     * - isRead는 사용자가 실제 확인할 때까지 false 유지
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행 (60초)
    @Transactional(timeout = 30)
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();

        try {
            // 발송 예정인 스케줄 알림 조회 (isSent = false인 것만)
            List<Notification> dueNotifications = notificationRepository.findDueScheduleNotifications(now);

            if (dueNotifications.isEmpty()) {
                return; // 발송할 알림이 없으면 조기 종료
            }

            log.info("발송 예정 알림 {}개 처리 시작", dueNotifications.size());

            int successCount = 0;
            int failCount = 0;

            for (Notification notification : dueNotifications) {
                try {
                    // 실시간 알림 발송 (내부에서 isSent = true, sentAt 설정됨)
                    notificationService.sendRealTimeNotification(notification);
                    successCount++;

                } catch (Exception e) {
                    log.error("알림 발송 실패: notificationId={}, error={}",
                            notification.getId(), e.getMessage(), e);
                    failCount++;

                    // 발송 실패한 알림은 isSent가 false로 유지되어 다음 주기에 재시도됨
                }
            }

            log.info("알림 처리 완료: 성공={}개, 실패={}개", successCount, failCount);

        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 만료된 알림 정리 (선택사항)
     *
     * 매일 자정에 실행되어 오래된 알림들을 정리
     * - 30일 이전의 읽은 알림들을 삭제하여 DB 용량 관리
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 실행
    @Transactional(timeout = 60)
    public void cleanupOldNotifications() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

            // 30일 이전의 읽은 알림들 삭제 (향후 구현)
            // int deletedCount = notificationRepository.deleteOldReadNotifications(thirtyDaysAgo);
            // log.info("오래된 알림 {}개 정리 완료", deletedCount);

            log.info("오래된 알림 정리 작업 실행 (구현 예정)");

        } catch (Exception e) {
            log.error("알림 정리 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}