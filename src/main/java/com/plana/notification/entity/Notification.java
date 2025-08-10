package com.plana.notification.entity;

import com.plana.auth.entity.Member;
import com.plana.calendar.entity.ScheduleAlarm;
import com.plana.diary.entity.DiaryTag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통합 알림 엔티티
 * 
 * 역할:
 * - 다이어리 태그 알림과 일정(Calendar) 알림을 통합 관리
 * - ScheduleAlarm은 "설정값"만 저장하고, 실제 알림 발송 시각은 이 엔티티에서 계산/관리
 * - 실제 알림 시스템 구현의 핵심 엔티티
 * 
 * 알림 처리 흐름:
 * 1. ScheduleAlarm: 일정별로 "5분전", "1시간전" 등 상대적 알림 설정 저장
 * 2. Notification: 실제 알림 발송 시각 계산하여 저장 (일정시작시간 - notifyBeforeVal * notifyUnit)
 * 3. 알림 스케줄러: 이 엔티티의 time 필드 기준으로 실제 알림 발송
 * 
 * 통합 설계 이유:
 * - 다이어리 태그 알림과 일정 알림을 하나의 알림함에서 통합 관리
 * - 공통 알림 처리 로직 (읽음/안읽음, 발송시간, 알림 히스토리 등) 통일
 */
@Entity
@Table(name = "notification")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    
    // 내부 식별자(PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 일정 알림 참조 (FK: schedule_alarm_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_alarm_id")
    private ScheduleAlarm scheduleAlarm;
    
    // 다이어리 태그 참조 (FK: diary_tag_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_tag_id")
    private DiaryTag diaryTag;
    
    // 알림 받을 사용자 (FK: member_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    // 알림 유형 (TAG / ALARM)
    @Column(nullable = false, length = 20)
    private String type;
    
    // 실제 알림 발생 시각
    @Column(nullable = false)
    private LocalDateTime time;
    
    // 읽음 여부
    @Column(nullable = false)
    private Boolean isRead;
    
    // 읽음 시간
    @Column
    private LocalDateTime readAt;
    
    // 알림 생성 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}