package com.plana.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberInfoDto {
    private Long id;
    private String login_id;
    private String email;
    private String name;
    private String nickname;
    private String provider; // LOCAL, GOOGLE, KAKAO 등
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
