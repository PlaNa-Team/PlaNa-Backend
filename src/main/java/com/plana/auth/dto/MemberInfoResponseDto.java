package com.plana.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberInfoResponseDto {

    private Long id;

    @JsonProperty("login_id")
    private String loginId;

    private String name;

    private String email;

    private String nickname;

    private String provider;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
