package com.plana.diary.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

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
    @JoinColumn(name="diary_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Diary diary;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String director;

    @Column(columnDefinition = "TEXT")
    private String actors;

    @Column(length = 50)
    private String genre;

    @Column(name = "rewatch", nullable = false)
    private boolean rewatch = false;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "release_date") // ERD: DATE
    private LocalDate releaseDate;
}
