package com.plana.auth.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 일반 회원가입 요청 DTO
 * 이메일, 비밀번호, 이름(+아이디, 닉네임, provider)으로 회원가입 처리
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignupRequestDto {

    // 로그인 아이디
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "아이디는 영문 소문자, 숫자, 밑줄만 가능합니다")
    private String loginId;

    // 이메일 (로그인 ID로도 사용 가능)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String email;

    // 비밀번호
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$",
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

    // 닉네임
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
    private String nickname;

    // 가입 제공자(LOCAL/GOOGLE/KAKAO/NAVER/APPLE)
    @NotNull(message = "provider는 필수입니다")
    private Provider provider;

    /** 비밀번호와 비밀번호 확인이 일치하는지 검증 */
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다")
    public boolean isPasswordMatch() {
        return password != null && password.equals(passwordConfirm);
    }

    /** 입력값 정규화(공백 제거, 이메일 소문자) — 서비스단 진입 전에 호출해도 좋음 */
    public void normalize() {
        if (loginId != null)   loginId = loginId.trim();
        if (email != null)     email = email.trim().toLowerCase();
        if (name != null)      name = name.trim();
        if (nickname != null)  nickname = nickname.trim();
    }

    /** 인증/소셜 제공자 타입 */
    public enum Provider {
        LOCAL, GOOGLE, KAKAO, NAVER;

        /** 대소문자 섞여 들어와도 매핑되도록 처리 */
        @JsonCreator
        public static Provider from(String v) {
            return v == null ? null : Provider.valueOf(v.trim().toUpperCase());
        }
        @JsonValue
        public String toValue() { return name(); }
    }
}
