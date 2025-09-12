package com.plana.diary.dto.response;

import com.plana.diary.enums.TagStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Builder
public class CreateDiaryTagResponseDto {
    private Long memberId;
    private String tagText;
    private TagStatus tagStatus;
}
