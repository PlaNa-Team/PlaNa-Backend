package com.plana.auth.repository;

import com.plana.auth.entity.User;
import com.plana.auth.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보 데이터베이스 접근을 위한 Repository
 * 소셜 로그인 관련 사용자 조회 메서드 제공
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 이메일로 사용자 조회 (로그인 시 사용)
    Optional<User> findByEmail(String email);
    
    // 소셜 제공업체와 제공업체 ID로 사용자 조회 (소셜 로그인 시 사용)
    Optional<User> findByProviderAndProviderId(SocialProvider provider, String providerId);
    
    // 이메일과 소셜 제공업체로 사용자 조회 (같은 이메일의 다른 소셜 계정 확인)
    Optional<User> findByEmailAndProvider(String email, SocialProvider provider);
    
    // 이메일 중복 확인 (회원가입 시 사용)
    boolean existsByEmail(String email);
    
    // 소셜 계정 중복 확인 (같은 소셜 계정으로 이미 가입했는지 확인)
    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);
}
