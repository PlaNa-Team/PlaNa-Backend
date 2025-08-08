package com.plana.diary.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCreateResponseDto {
    private Long id;

    @JsonProperty("diary_date") // JSON에서는 diary_date로 보이게 한다.
    private LocalDate diaryDate;

    @JsonProperty("diary_type")
    private String type;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private String imageUrl;

    private Object content;

    private List<CreateDiaryTagResponseDto> diaryTags;
}
