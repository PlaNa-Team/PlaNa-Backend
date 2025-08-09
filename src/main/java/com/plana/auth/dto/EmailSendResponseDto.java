package com.plana.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailSendResponseDto {

    private int status;
    private boolean duplicate;
    private String message;

    public static EmailSendResponseDto duplicated() {
        return EmailSendResponseDto.builder()
                .status(HttpStatus.CONFLICT.value())
                .duplicate(true)
                .message("이미 가입된 이메일입니다.")
                .build();
    }

    public static EmailSendResponseDto sent() {
        return EmailSendResponseDto.builder()
                .status(HttpStatus.OK.value())
                .duplicate(false)
                .message("인증번호가 이메일로 전송되었습니다.")
                .build();
    }
}
