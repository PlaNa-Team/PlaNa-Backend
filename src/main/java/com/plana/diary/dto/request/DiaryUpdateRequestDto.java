package com.plana.diary.dto.request;

import com.plana.diary.enums.DiaryType;
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
    private LocalDate diaryDate;
    private String imageUrl;
    private DiaryType diaryType;
    // 타입별 본문
    private Object content;

    //태그 전체 교체
    @Size(min=0)
    private List<DiaryTagRequestDto> diaryTags;

    //클라이언트가 마지막으로 본 버전
//    private Long version;
}
