package com.plana.diary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MovieContentRequestDto {
    private String title;
    private String director;
    private String actors;
    private String genre;
    private boolean rewatch;
    private Integer rating;
    private String comment;
}
