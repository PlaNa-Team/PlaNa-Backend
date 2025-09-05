package com.plana.diary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MovieContentRequestDto {
    @NotBlank(message = "영화제목을 작성해주세요.")
    private String title;
    private String director;
    private String actors;
    private String genre;
    private boolean rewatch;
    private Integer rating;
    private String comment;
}
