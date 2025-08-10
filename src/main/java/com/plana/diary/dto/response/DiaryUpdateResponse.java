package com.plana.diary.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiaryUpdateResponse {
    private int status;
    private Body body;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Body {
        private DiaryDetailResponseDto data;
    }
}
