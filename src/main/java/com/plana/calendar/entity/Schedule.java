package com.plana.calendar.entity;

import com.plana.auth.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/* 일정 정보를 저장하는 메인 엔티티 */
@Entity
@Table(name = "schedule")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Schedule {
    
    // 내부 식별자(PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 작성자 (FK: member_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    // 카테고리 선택(FK: category_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    // 일정 제목
    @Column(nullable = false, length = 100)
    private String title;
    
    // 일정 색상
    @Column(length = 20)
    private String color;
    
    // 일정 상세 설명
    @Column(length = 255)
    private String description;
    
    // 일정 시작 시각
    @Column(nullable = false)
    private LocalDateTime startAt;
    
    // 일정 종료 시각
    @Column
    private LocalDateTime endAt;
    
    // 일정 종일 여부 확인 (FALSE(기본값))
    @Builder.Default
    @Column(nullable = false)
    private Boolean isAllDay = false;
    
    // 반복 여부 확인 (FALSE(기본값))
    @Builder.Default
    @Column(nullable = false)
    private Boolean isRecurring = false;
    
    // 반복 규칙 (매일, 매주, 매월, 매년) - RFC 5545 RRule 형식 (예: "FREQ=WEEKLY;BYDAY=WE")
    @Column(length = 255)
    private String recurrenceRule;
    
    // 반복 종료일
    @Column
    private LocalDateTime recurrenceUntil;
    
    // 일정 생성 일시
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // 일정 수정 일시
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 일정 삭제 여부 (FALSE(기본값))
    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}