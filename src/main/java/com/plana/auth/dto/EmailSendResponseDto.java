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

    // 회원가입: 중복된 이메일
    public static EmailSendResponseDto duplicated() {
        return EmailSendResponseDto.builder()
                .status(HttpStatus.CONFLICT.value()) // 409
                .duplicate(true)
                .message("이미 가입된 이메일입니다.")
                .build();
    }

    // 공통: 인증번호 정상 발송
    public static EmailSendResponseDto sent() {
        return EmailSendResponseDto.builder()
                .status(HttpStatus.OK.value()) // 200
                .duplicate(false)
                .message("인증번호가 이메일로 전송되었습니다.")
                .build();
    }

    // 아이디/비밀번호 찾기: 가입되지 않은 이메일
    public static EmailSendResponseDto notFound() {
        return EmailSendResponseDto.builder()
                .status(HttpStatus.NOT_FOUND.value()) // 404
                .duplicate(false)
                .message("가입되지 않은 이메일입니다.")
                .build();
    }
}
