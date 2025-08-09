package com.plana.diary.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DiaryMonthlyResponseDto {
    private List<DiaryMonthlyItemDto> diaryList;
}
