package com.plana.diary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookContentRequestDto {
    private String title;
    private String author;
    private String publisher;
    private String genre;
    private Integer rating;
    private String comment;

    private LocalDate startDate;
    private LocalDate endDate;
}
