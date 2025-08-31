package com.plana.auth.dto;

import lombok.Builder;
import lombok.Getter;

// 토큰 번들 DTO (응답 바디에는 refreshToken을 넣지 말고, 컨트롤러에서만 사용)
@Getter
@Builder
public class IssuedTokens {
    private final String accessToken;
    private final long   accessExpiresInSec;
    private final String refreshToken;          // 컨트롤러에서만 쿠키로 사용
    private final long   refreshMaxAgeSec;      // 쿠키 maxAge 계산용
    private final MemberInfoDto member;         // 기존 응답에 필요하면 포함
}

