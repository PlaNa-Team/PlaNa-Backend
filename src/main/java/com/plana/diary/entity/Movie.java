package com.plana.diary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="diary_id", nullable = false)
    private Diary diary;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 50)
    private String director;

    @Column(columnDefinition = "TEXT")
    private String actors;

    @Column(length = 50)
    private String genre;

    @Column(nullable = false)
    private boolean rewatched = false;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
