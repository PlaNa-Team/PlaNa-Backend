package com.plana.calendar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/* 일정 카테고리 정보를 저장하는 엔티티, 사용자가 직접 만들어 사용하는 카테고리 */
@Entity
@Table(name = "category")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Category {
    
    // 내부 식별자(PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 카테고리 이름
    @Column(nullable = false, length = 100)
    private String name;
    
    // 카테고리 색상 코드
    @Column(nullable = false, length = 50)
    private String color;
    
    // 카테고리 삭제 여부 (FALSE(기본값))
    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;
}