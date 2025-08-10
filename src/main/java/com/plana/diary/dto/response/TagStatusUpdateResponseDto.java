package com.plana.diary.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagStatusUpdateResponseDto {
    private Long id;
    private String tagStatus;
    private LocalDateTime updatedAt;
    private DiaryDetailResponseDto diary;
}
