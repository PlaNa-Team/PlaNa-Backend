package com.plana.auth.service;

import com.plana.auth.dto.LoginRequestDto;
import com.plana.auth.dto.LoginResponseDto;
import com.plana.auth.dto.SignupRequestDto;
import com.plana.auth.dto.SignupResponseDto;
import com.plana.auth.entity.Member;
import com.plana.auth.enums.SocialProvider;
import com.plana.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일반 회원가입/로그인 비즈니스 로직 서비스
 * 기존 소셜 로그인 시스템과 완전 호환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redis;

    /**
     * 일반 회원가입 처리
     * @param signupRequest 회원가입 요청 정보
     * @return 회원가입 응답 정보
     * @throws IllegalArgumentException 이메일 중복, 비밀번호 불일치 등
     */
    @Transactional
    public SignupResponseDto signup(SignupRequestDto signupRequest) {
        String email = signupRequest.getEmail().trim().toLowerCase();

        log.info("일반 회원가입 시도: {}", signupRequest.getEmail());
        
        // 1. 비밀번호 일치 검증
        if (!signupRequest.isPasswordMatch()) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        // 2. 이메일 인증 완료 여부(서버 판정: Redis 플래그 확인)
        String verifiedKey = "email:verify:ok:" + email;
        String verified = redis.opsForValue().get(verifiedKey);
        if (!"true".equals(verified)) {
            // 400/403/409 등 팀 규칙에 맞게 예외 타입/상태코드 매핑
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다");
        }
        
        // 3. 이메일 중복 재검증 (소셜 로그인 포함)
        if (memberRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다");
        }

        // 4. 아이디 중복 재검증
        if (signupRequest.getLoginId() != null &&
                memberRepository.existsByLoginId(signupRequest.getLoginId())) {
            throw new IllegalArgumentException("이미 사용중인 아이디입니다");
        }
        
        // 5. 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(signupRequest.getPassword());
        
        // 6. Member 엔티티 생성
        Member newMember = Member.builder()
                .email(signupRequest.getEmail())
                .loginId(signupRequest.getLoginId())
                .name(signupRequest.getName())
                .nickname(signupRequest.getNickname())
                .password(encryptedPassword) // 암호화된 비밀번호
                .provider(SocialProvider.LOCAL) // 일반 로그인 구분
                .providerId(null) // 일반 로그인은 providerId 없음
                .role("ROLE_USER") // 기본 권한
                .enabled(true) // 계정 활성화
                .build();
        
        // 7. 데이터베이스 저장
        Member savedMember = memberRepository.save(newMember);
        
        log.info("일반 회원가입 완료: memberId={}, email={}", savedMember.getId(), savedMember.getEmail());
        
        // 8. 응답 dto 반환
        return SignupResponseDto.builder()
                .status(201)
                .message("회원가입 성공")
                .data(SignupResponseDto.DataDto.builder()
                        .id(savedMember.getId())
                        .name(savedMember.getName())
                        .login_id(savedMember.getLoginId())
                        .email(savedMember.getEmail())
                        .nickname(savedMember.getNickname())
                        .provider(savedMember.getProvider().name())
                        .created_at(savedMember.getCreatedAt())
                        .updated_at(savedMember.getUpdatedAt())
                        .build())
                .build();
    }

    /**
     * 일반 로그인 처리
     * @param loginRequest 로그인 요청 정보
     * @return 로그인 응답 정보 (JWT 토큰 포함)
     * @throws IllegalArgumentException 로그인 실패 (이메일 없음, 비밀번호 불일치 등)
     */
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        log.info("일반 로그인 시도: {}", loginRequest.getEmail());
        
        // 1. 이메일로 사용자 조회
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));
        
        // 2. 일반 로그인 사용자인지 확인 (소셜 로그인 사용자는 password가 null)
        if (member.getProvider() != SocialProvider.LOCAL || member.getPassword() == null) {
            throw new IllegalArgumentException("소셜 로그인으로 가입된 계정입니다. 소셜 로그인을 사용해주세요");
        }
        
        // 3. 계정 활성화 상태 확인
        if (!member.getEnabled()) {
            throw new IllegalArgumentException("비활성화된 계정입니다. 관리자에게 문의하세요");
        }
        
        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        
        // 5. JWT 토큰 생성 (기존 소셜 로그인과 동일한 방식)
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole()
        );
        
        log.info("일반 로그인 성공: memberId={}, email={}", member.getId(), member.getEmail());
        
        // 6. 응답 DTO 생성 (소셜 로그인과 동일한 구조)
        LoginResponseDto.MemberInfoDto memberInfo = LoginResponseDto.MemberInfoDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .provider(member.getProvider().getValue())
                .build();
        
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .expiresIn(3600L) // 1시간
                .member(memberInfo)
                .build();
    }

    /**
     * 이메일 중복 검사
     * @param email 검사할 이메일
     * @return 중복이면 true, 사용 가능하면 false
     */
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return memberRepository.existsByEmail(email);
    }

    /**
     * 사용자 ID로 사용자 정보 조회
     * @param memberId 사용자 ID
     * @return 사용자 정보
     * @throws IllegalArgumentException 사용자 없음
     */
    @Transactional(readOnly = true)
    public Member getUserById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }

    /**
     * 이메일로 사용자 정보 조회
     * @param email 사용자 이메일
     * @return 사용자 정보
     * @throws IllegalArgumentException 사용자 없음
     */
    @Transactional(readOnly = true)
    public Member getUserByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }

    // 회원가입 시 아이디 중복 확인
    @Transactional(readOnly = true)
    public boolean isLoginIdExists(String loginId) {
        return !memberRepository.existsByLoginId(loginId);
    }
}
