package com.plana.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 성공 응답 DTO
 * 기존 JWT 토큰 시스템과 동일한 구조 사용
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponseDto {
    
    // JWT 액세스 토큰
    private String accessToken;
    
    // 토큰 만료 시간 (초 단위)
    @Builder.Default
    private Long expiresIn = 3600L; // 1시간
    
    // 사용자 정보
    private MemberInfoDto member;
    
    // 응답 시간
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();
}
