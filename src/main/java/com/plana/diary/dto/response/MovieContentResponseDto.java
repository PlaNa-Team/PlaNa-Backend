package com.plana.diary.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieContentResponseDto implements DiaryContentResponseDto{
   private String title;
   private String director;
   private String actors;
   private String genre;
   private boolean rewatch;
   private Integer rating;
   private String comment;
}
