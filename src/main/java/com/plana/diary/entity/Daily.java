package com.plana.diary.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Daily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Diary와 1대1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id" , nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Diary diary;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String memo;

}

