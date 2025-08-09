package com.plana.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원가입 성공 응답 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignupResponseDto {

    private int status;      // 201
    private String message;  // "회원가입 성공"
    private DataDto data;    // 회원 정보
    @Builder.Default
    private Long timestamp = System.currentTimeMillis(); // 응답시간

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DataDto {
        private Long id;
        private String name;
        private String login_id;
        private String email;
        private String nickname;
        private String provider;
        private LocalDateTime created_at;
        private LocalDateTime updated_at;
    }
}
