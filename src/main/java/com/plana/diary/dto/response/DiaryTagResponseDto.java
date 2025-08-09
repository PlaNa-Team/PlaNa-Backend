package com.plana.diary.dto.response;

import com.plana.diary.enums.TagStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryTagResponseDto {
    private Long id;
    private Long memberId;
    private String loginId;
    private String memberNickname;
    private String tagText;
    private TagStatus tagStatus;
}
