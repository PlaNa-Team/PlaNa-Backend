package com.plana.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 일반 회원가입 요청 DTO
 * 이메일, 비밀번호, 이름으로 회원가입 처리
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignupRequestDto {
    
    // 이메일 (로그인 ID로 사용)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String email;
    
    // 비밀번호
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$",
        message = "비밀번호는 영문과 숫자를 포함해야 합니다"
    )
    private String password;
    
    // 비밀번호 확인
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String passwordConfirm;
    
    // 사용자 이름
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    private String name;
    
    /**
     * 비밀번호와 비밀번호 확인이 일치하는지 검증
     * @return 일치하면 true, 불일치하면 false
     */
    public boolean isPasswordMatch() {
        return password != null && password.equals(passwordConfirm);
    }
}
