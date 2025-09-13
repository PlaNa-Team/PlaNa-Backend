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
    private Long id;
    private Long memberId;
    private String loginId;
    private String memberNickname;
    private String tagText;
    private TagStatus tagStatus;
}
