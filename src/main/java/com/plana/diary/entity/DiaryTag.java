package com.plana.diary.entity;

import com.plana.auth.entity.Member;
import com.plana.diary.enums.TagStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "diary_tag")
public class DiaryTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //어떤 다이어리에 대한 태그인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    //태그된 사용자 (공유 대상자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Member member;

    //상태: 작성자, 미설정, 수락, 거절, 삭제
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TagStatus tagStatus;

    //자동완성 없이 사용자가 직접 이력한 경우 저장
    @Column(length = 100)
    private String tagText;
}
