package com.plana.diary.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryUpdateRequestDto {
    @NotNull private LocalDate diaryDate;
    private String imageUrl;

    // 타입별 본문
    @NotNull private Object content;

    //태그 전체 교체
    @NotNull @Size(min=0)
    private List<DiaryTagRequestDto> diaryTags;
}
