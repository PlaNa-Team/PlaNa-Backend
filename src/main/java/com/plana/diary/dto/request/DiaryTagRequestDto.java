package com.plana.diary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryTagRequestDto {
    private Long memberId;
    private Long diaryId;
    private String tagText;
}
