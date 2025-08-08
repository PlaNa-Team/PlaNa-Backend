package com.plana.diary.dto.response;

import com.plana.diary.enums.DiaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryDetailResponseDto {
    private Long id;
    private LocalDate diaryDate;
    private DiaryType diaryType;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 일상/ 책/ 영화
    private DiaryContentResponseDto content;

    // 태그
    private List<DiaryTagResponseDto> diaryTags;
}
