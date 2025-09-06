package com.plana.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 현재 비밀번호 확인
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordConfirmRequestDto {
    @NotBlank
    private String currentPassword;
}