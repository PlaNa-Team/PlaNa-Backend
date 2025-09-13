package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공통 API 응답 래퍼 DTO
 * 
 * 사용 API: 모든 Calendar API 응답
 * - GET /api/calendars (월별 일정 조회)
 * - GET /api/calendars/{id} (상세 조회)
 * - POST /api/calendars (생성)
 * - PUT /api/calendars/{id} (수정)
 * - DELETE /api/calendars/{id} (삭제)
 * 
 * 사용 패턴: Diary 모듈과 동일한 공통 응답 구조
 * Generic 타입으로 다양한 Response DTO를 감쌀 수 있음
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private String error;
    
    // 성공 응답 생성 메서드
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, null);
    }
    
    // 생성 성공 응답 생성 메서드
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, message, data, null);
    }
    
    // 오류 응답 생성 메서드
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null, null);
    }
    
    // 오류 응답 생성 메서드 (error 타입 포함)
    public static <T> ApiResponse<T> error(int status, String message, String errorType) {
        return new ApiResponse<>(status, message, null, errorType);
    }
}