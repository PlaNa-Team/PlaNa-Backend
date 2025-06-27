package com.plana.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원가입 성공 응답 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignupResponseDto {
    
    // 성공 메시지
    private String message;
    
    // 생성된 사용자 ID
    private Long memberId;
    
    // 생성된 사용자 이메일
    private String email;
    
    // 생성된 사용자 이름
    private String name;
    
    // 응답 시간
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();
}
