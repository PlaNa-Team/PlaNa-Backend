package com.plana.notification.service;

import com.plana.notification.dto.response.NotificationListResponseDto;
import com.plana.notification.dto.response.NotificationResponseDto;
import com.plana.notification.entity.Notification;
import org.springframework.data.domain.Pageable;

/**
 * 알림 서비스 인터페이스
 */
public interface NotificationService {

    /**
     * 사용자의 알림 목록 조회
     *
     * @param memberId 사용자 ID
     * @param pageable 페이징 정보
     * @param unreadOnly 안읽은 알림만 조회할지 여부
     * @return 알림 목록과 페이징 정보
     */
    NotificationListResponseDto getNotifications(Long memberId, Pageable pageable, boolean unreadOnly);

    /**
     * 개별 알림 읽음 처리
     *
     * @param notificationId 알림 ID
     * @param memberId 사용자 ID (권한 확인용)
     * @return 업데이트된 알림 정보
     */
    NotificationResponseDto markAsRead(Long notificationId, Long memberId);

    /**
     * 사용자의 모든 안읽은 알림을 읽음 처리
     *
     * @param memberId 사용자 ID
     * @return 읽음 처리된 알림 개수
     */
    int markAllAsRead(Long memberId);

    /**
     * 다이어리 태그 알림 생성
     *
     * @param diaryTagId 다이어리 태그 ID
     * @param targetMemberId 알림을 받을 사용자 ID
     * @param message 알림 메시지
     * @return 생성된 알림 정보
     */
    NotificationResponseDto createDiaryTagNotification(Long diaryTagId, Long targetMemberId, String message);

    /**
     * 스케줄 알림 생성
     *
     * @param scheduleAlarmId 스케줄 알람 ID
     * @param targetMemberId 알림을 받을 사용자 ID
     * @param message 알림 메시지
     * @return 생성된 알림 정보
     */
    NotificationResponseDto createScheduleNotification(Long scheduleAlarmId, Long targetMemberId, String message);

    /**
     * 실시간 알림 발송 (WebSocket)
     *
     * @param notification 발송할 알림
     */
    void sendRealTimeNotification(Notification notification);

    /**
     * 사용자의 안읽은 알림 개수 조회
     *
     * @param memberId 사용자 ID
     * @return 안읽은 알림 개수
     */
    long getUnreadCount(Long memberId);
}