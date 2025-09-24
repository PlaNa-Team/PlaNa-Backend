package com.plana.auth.dto;

import com.plana.auth.enums.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailSendRequestDto {
    @NotBlank
    @Email
    String email;

    private VerificationPurpose purpose;
}
