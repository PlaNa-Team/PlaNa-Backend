package com.plana.auth.dto;

import com.plana.auth.enums.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증된 사용자 정보 DTO
 * Spring Security의 @AuthenticationPrincipal로 사용되는 경량화된 사용자 정보
 * LazyInitializationException 방지를 위해 연관관계 필드 제외
 */
@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class AuthenticatedMemberDto {
    private Long id;       /* 회원 고유 코드 (Primary Key) */
    private String name;
    private String email;
    private String role;        /* 사용자 권한 (ROLE_USER, ROLE_ADMIN 등) */
    private SocialProvider provider;
    // private Boolean enabled;    /* OAuth2 이용시 계정 활성화 여부에 사용됨 */
    private boolean isDeleted;
}
