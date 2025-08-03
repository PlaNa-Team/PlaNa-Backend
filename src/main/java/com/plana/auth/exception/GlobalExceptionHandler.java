package com.plana.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * Bean Validation 오류 등을 일관되게 JSON 형태로 응답
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Bean Validation 오류 처리
     * @Valid 어노테이션으로 인한 검증 실패 시 발생
     * @param ex MethodArgumentNotValidException
     * @return JSON 형태의 오류 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        log.warn("Bean Validation error occurred: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        
        // 첫 번째 검증 오류 메시지 사용
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다");

        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", errorMessage);
        response.put("timestamp", System.currentTimeMillis());
        response.put("error", "Validation Failed");
        
        log.debug("Validation error details: {}", ex.getBindingResult().getAllErrors());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")  // 명시적 Content-Type 설정
                .body(response);
    }
    
    /**
     * 일반적인 IllegalArgumentException 처리
     * 비즈니스 로직에서 발생하는 예외
     * @param ex IllegalArgumentException
     * @return JSON 형태의 오류 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        log.warn("Business logic error occurred: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());
        response.put("error", "Bad Request");
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body(response);
    }
    
    /**
     * 기타 예상치 못한 예외 처리
     * @param ex Exception
     * @return JSON 형태의 오류 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        
        log.error("Unexpected error occurred", ex);
        
        Map<String, Object> response = new HashMap<>();

        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "서버에서 오류가 발생했습니다");
        response.put("timestamp", System.currentTimeMillis());
        response.put("error", "Internal Server Error");
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body(response);
    }
}
