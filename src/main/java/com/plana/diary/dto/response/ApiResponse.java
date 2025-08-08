package com.plana.diary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private Body<T> body;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body<T> {
        private T data;
    }

    // 성공 응답 생성
    public static <T> ApiResponse<T> success(int status, T data) {
        return new ApiResponse<>(status, null, new Body<>(data));
    }

    // 실패 응답 생성
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
