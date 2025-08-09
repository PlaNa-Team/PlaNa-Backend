package com.plana.diary.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookContentResponseDto implements DiaryContentResponseDto {
    private String title;
    private String author;
    private String publisher;
    private String genre;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer rating;
    private String comment;
}
