package com.plana.diary.dto.request;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "책 제목을 작성해주세요.")
    private String title;

    @NotBlank(message = "저자를 입력해주세요.")
    private String author;
    private String publisher;
    private String genre;
    private Integer rating;
    private String comment;

    private LocalDate startDate;
    private LocalDate endDate;
}
