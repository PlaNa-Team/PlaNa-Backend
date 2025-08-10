package com.plana.auth.repository;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.auth.entity.Member;
import com.plana.auth.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보 데이터베이스 접근을 위한 Repository
 * 소셜 로그인 관련 사용자 조회 메서드 제공
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT new com.plana.auth.dto.AuthenticatedMemberDto(" +
            "m.id, m.name, m.email, m.role, m.provider, m.isDeleted)" + // \" +, m.enabled) " +
            "FROM Member m WHERE m.id = :id")
    Optional<AuthenticatedMemberDto> findAuthenticatedMemberById(Long id);

    // 이메일로 사용자 조회 (로그인 시 사용)
    Optional<Member> findByEmail(String email);
    
    // 소셜 제공업체와 제공업체 ID로 사용자 조회 (소셜 로그인 시 사용)
    Optional<Member> findByProviderAndProviderId(SocialProvider provider, String providerId);
    
    // 이메일과 소셜 제공업체로 사용자 조회 (같은 이메일의 다른 소셜 계정 확인)
    Optional<Member> findByEmailAndProvider(String email, SocialProvider provider);
    
    // 이메일 중복 확인 (회원가입 시 사용)
    boolean existsByEmail(String email);
    
    // 소셜 계정 중복 확인 (같은 소셜 계정으로 이미 가입했는지 확인)
    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);

    // 아이디 중복 확인 (회원가입 시 사용)
    boolean existsByLoginId(String loginId);

    @Query(value = "SELECT * FROM member WHERE email = :email", nativeQuery = true)
    Optional<Member> findByEmailIncludingDeleted(@Param("email") String email);
}
