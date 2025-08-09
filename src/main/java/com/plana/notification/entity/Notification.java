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

/* 다이어리 태그와 일정 알림을 통합 관리하는 엔티티 */
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