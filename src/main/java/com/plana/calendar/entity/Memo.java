package com.plana.calendar.entity;

import com.plana.auth.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메모 정보를 저장하는 엔티티
 * 
 * 테이블 설계:
 * - id: 내부 식별자 (PK)
 * - member_id: 작성자 (FK: member_id)
 * - content: 메모 내용 (최대 255자)
 * - created_at: 메모 작성 시각
 * - updated_at: 메모 수정 시각
 * - is_deleted: 일정 삭제 여부 (논리적 삭제, 기본값 FALSE)
 * - year: 주차를 식별하기위한 연도
 * - week: ISO 국제 표준 주차 번호 (1-53)
 * - type: 메모 타입 (다이어리/스케줄)
 */
@Entity
@Table(name = "memo")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Memo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 작성자 (FK: member_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    // 메모 내용
    @Column(nullable = false, length = 255)
    private String content;
    
    // 메모 작성 시각
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 메모 수정 시각  
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 일정 삭제 여부 (FALSE(기본값))
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
    
    // 주차를 식별하기위한 연도
    @Column
    private Short year;
    
    // ISO 국제 표준 주차 번호 (1-53)
    @Column
    private Short week;
    
    // 메모 타입
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MemoType type;
    
    /**
     * 메모 타입 열거형
     */
    public enum MemoType {
        다이어리, 스케줄
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}