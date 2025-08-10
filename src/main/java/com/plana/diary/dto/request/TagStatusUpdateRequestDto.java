package com.plana.diary.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TagStatusUpdateRequestDto {
    // 수락 또는 거절
    private String tagStatus;
}
