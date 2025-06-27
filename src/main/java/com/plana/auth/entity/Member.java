package com.plana.auth.entity;

import com.plana.auth.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/* 사용자 정보를 저장하는 엔티티, 소셜 로그인과 일반 로그인을 모두 지원 */
@Entity
@Table(name = "member")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member {
    
    // 사용자 고유 식별자 (자동 증가)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 사용자 이메일 (로그인 ID로 사용, 중복 불가)
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;
    
    // 비밀번호 (일반 로그인용, 소셜 로그인시 null)
    @Column(length = 255)
    private String password;
    
    // 프로필 이미지 URL (소셜 로그인 시 제공받음)
    @Column(length = 500)
    private String profileImageUrl;
    
    // 가입 경로 (GOOGLE, KAKAO, NAVER, LOCAL)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider; // SocialProvider Enum 사용

    @Column(length = 100)
    private String providerId; // 소셜 서비스에서 제공하는 고유 ID

    // 사용자 권한 (ROLE_USER: 일반 사용자, ROLE_ADMIN: 관리자)
    @Builder.Default
    @Column(nullable = false)
    private String role = "ROLE_USER"; // 권한 (ROLE_USER, ROLE_ADMIN), 권한 관리 (일반 사용자, 관리자)

    // 계정 활성화 여부 (false: 비활성화된 계정)
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    // 계정 생성 시간 (변경 불가)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // 마지막 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 엔티티 저장 전 자동 실행 (생성 시간 설정)
    @PrePersist
    protected void onCreate() { // 생성 시간 자동 설정
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    // 엔티티 수정 전 자동 실행 (수정 시간 갱신)
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    } // 수정 시간 자동 설정
}
