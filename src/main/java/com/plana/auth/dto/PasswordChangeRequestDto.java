package com.plana.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 비밀번호 변경
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequestDto {
    @NotBlank private String newPassword;
    @NotBlank private String confirmPassword;
}
