package com.plana.diary.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DiaryMonthlyItemDto {
    private Long id;
    private LocalDate diaryDate;
    private String type;
    private String imageUrl;
    private String title;
}
