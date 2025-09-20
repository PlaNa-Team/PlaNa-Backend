package com.plana.notification.repository;

import com.plana.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 특정 사용자의 알림 목록 조회 (페이징)
     */
    @Query("SELECT n FROM Notification n WHERE n.member.id = :memberId ORDER BY n.createdAt DESC")
    Page<Notification> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 사용자의 안읽은 알림만 조회 (페이징)
     */
    @Query("SELECT n FROM Notification n WHERE n.member.id = :memberId AND n.isRead = false ORDER BY n.createdAt DESC")
    Page<Notification> findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 사용자의 안읽은 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.member.id = :memberId AND n.isRead = false")
    long countByMemberIdAndIsReadFalse(@Param("memberId") Long memberId);

    /**
     * 특정 사용자의 모든 알림을 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.member.id = :memberId AND n.isRead = false")
    int markAllAsReadByMemberId(@Param("memberId") Long memberId, @Param("readAt") LocalDateTime readAt);

    /**
     * 발송 예정인 스케줄 알림 조회 (스케줄러용)
     * time 필드가 현재 시간 이전이고 아직 발송되지 않은 알림들
     */
    @Query("SELECT n FROM Notification n WHERE n.type = 'ALARM' AND n.time <= :now AND n.isRead = false")
    List<Notification> findDueScheduleNotifications(@Param("now") LocalDateTime now);

    /**
     * 특정 다이어리 태그에 대한 알림 존재 여부 확인
     */
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.diaryTag.id = :diaryTagId")
    boolean existsByDiaryTagId(@Param("diaryTagId") Long diaryTagId);

    /**
     * 특정 스케줄 알람에 대한 알림 존재 여부 확인
     */
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.scheduleAlarm.id = :scheduleAlarmId")
    boolean existsByScheduleAlarmId(@Param("scheduleAlarmId") Long scheduleAlarmId);
}