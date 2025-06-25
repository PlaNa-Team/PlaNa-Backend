package com.plana.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 일반 로그인 요청 DTO
 * 이메일과 비밀번호로 로그인 처리
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequestDto {
    
    // 이메일 (로그인 ID)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String email;
    
    // 비밀번호
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 1, max = 255, message = "비밀번호를 입력해주세요")
    private String password;
}
