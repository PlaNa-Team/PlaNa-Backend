package com.plana.diary.entity;

import com.plana.auth.entity.Member;
import com.plana.diary.enums.DiaryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor // 기본 생성자를 만들어준다.
@AllArgsConstructor // 모든 필드를 매개변수로 받는 생성자
@Builder
public class Diary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 다이어리 기본키

    @Column(nullable=false)
    private LocalDate diaryDate; // 작성된 날짜

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private DiaryType type; // Daily, Book, Movie

    private String imageUrl; // 다이어리 배경 이미지 URL

    //다이어리입장에서 보는 것이다.
    @ManyToOne(fetch = FetchType.LAZY) // 작성자
    @JoinColumn(name = "member_id", nullable = false) // 외래키 이름 = member_id
    private Member writer;

    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime updatedAt; //수정 시간

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @PrePersist // 엔티티가 DB에 저장되기 전에 실행되는 메서드에 붙이는 어노테이션
    public void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now(); //최초 저장 시점
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(); // 업데이트 시점
    }

    public void markDeleted() {
        this.isDeleted = true;
    }

    // 낙관적 락용 버전 필드
//    @Version
//    private Long version;
}
