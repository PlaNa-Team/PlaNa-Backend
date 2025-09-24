package com.plana.diary.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Diary와 1대1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false, unique = true)
    @OnDelete(action= OnDeleteAction.CASCADE)
    private Diary diary;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(length = 50)
    private String genre;

    @Column(length = 50)
    private String publisher;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer rating;

    @Column(columnDefinition = "TEXT") // DB 컬럽 타입을 text로 지정하여 수만 자 가능하도록
    private String comment;

    @Column(name = "rewatch", nullable = false)
    private boolean rewatch = false;

}
