package com.plana.calendar.entity;

import com.plana.calendar.enums.NotifyUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/* 일정별 알림 설정을 저장하는 엔티티 */
@Entity
@Table(name = "schedule_alarm")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduleAlarm {
    
    // 내부 식별자(PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 일정 참조 (FK: schedule_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;
    
    // 알림 시간 숫자 값
    @Column
    private Integer notifyBeforeVal;
    
    // 알림 단위(분, 시간, 일)
    @Enumerated(EnumType.STRING)
    @Column
    private NotifyUnit notifyUnit;
}