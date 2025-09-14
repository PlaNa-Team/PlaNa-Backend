package com.plana.auth.service;

import com.plana.auth.dto.*;
import com.plana.auth.entity.Member;
import com.plana.auth.enums.SocialProvider;
import com.plana.auth.exception.ForbiddenException;
import com.plana.auth.exception.UnauthorizedException;
import com.plana.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

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
    private final EmailVerificationService emailVerificationService;

    private String okKey(Long memberId){ return "pwd:change:ok:" + memberId; }
    private static final Duration TTL = Duration.ofMinutes(5); // 5분 이내 변경

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityMs;

    @Value("${jwt.refresh-token-validity-remember}")
    private long refreshTokenValidityRememberMs;

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
                .data(MemberInfoDto.builder()
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
    public IssuedTokens login(LoginRequestDto loginRequest) {
        log.info("일반 로그인 시도: {}", loginRequest.getEmail());
        
        // 1. 이메일로 사용자 조회
        Member member = memberRepository.findByEmailIncludingDeleted(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // 2. 탈퇴한 사용자인지 확인
        if (member.isDeleted()) {
            throw new ForbiddenException("탈퇴한 계정입니다");
        }
        
        // 3. 일반 로그인 사용자인지 확인 (소셜 로그인 사용자는 password가 null)
        if (member.getProvider() != SocialProvider.LOCAL || member.getPassword() == null) {
            throw new UnauthorizedException("소셜 로그인으로 가입된 계정입니다. 소셜 로그인을 사용해주세요");
        }
        
        // 4. 계정 활성화 상태 확인
        if (!member.getEnabled()) {
            throw new ForbiddenException("비활성화된 계정입니다. 관리자에게 문의하세요");
        }
        
        // 5. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        
        // 6. JWT 토큰 생성 (기존 소셜 로그인과 동일한 방식)
        boolean remember = Boolean.TRUE.equals(loginRequest.getRememberMe());
        long rtMs = remember ? refreshTokenValidityRememberMs : refreshTokenValidityMs;

        String access = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole());
        String refresh = jwtTokenProvider.createRefreshToken(member.getId(), rtMs);

        long accessTtlSec = Math.max(0,
                (jwtTokenProvider.getExpirationDateFromToken(access).getTime() - System.currentTimeMillis()) / 1000);
        
        log.info("일반 로그인 성공: memberId={}, email={}", member.getId(), member.getEmail());
        
        // 7. 응답 DTO 생성 (소셜 로그인과 동일한 구조)
        MemberInfoDto memberInfo = MemberInfoDto.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .login_id(member.getLoginId())
                .email(member.getEmail())
                .name(member.getName())
                .provider(member.getProvider().getValue())
                .created_at(member.getCreatedAt())
                .updated_at(member.getUpdatedAt())
                .build();

        return IssuedTokens.builder()
                .accessToken(access)
                .accessExpiresInSec(accessTtlSec)
                .refreshToken(refresh)                    // 컨트롤러에서만 사용
                .refreshMaxAgeSec(rtMs / 1000)
                .member(memberInfo)
                .build();
    }

    /**
     * 본인 정보 조회
     * @param memberId 조회할 회원의 ID
     * @return MemberInfoResponseDto 회원의 상세 정보(id, loginId, name, email, nickname, provider, createdAt)
     * @throws IllegalArgumentException 해당 ID의 회원이 존재하지 않을 경우 발생
     */
    public MemberInfoResponseDto getMyInfo(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        return new MemberInfoResponseDto(
                m.getId(),
                m.getLoginId(),
                m.getName(),
                m.getEmail(),
                m.getNickname(),
                m.getProvider().name(),
                m.getCreatedAt()
        );
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

    // 회원 탈퇴
    @Transactional
    public void deleteMe(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnauthorizedException("인증이 필요합니다"));

        // 플래그 세팅
        m.setDeleted(true);
        m.setEnabled(false);

        // 토큰/세션 정리 (로그인에 redis 적용 시)
        // revokeTokensFor(m.getId());
    }

    /**
     * 사용자 닉네임 변경
     * @param memberId 사용자 고유번호
     * @param newNickname 변경한 닉네임
     * @throws IllegalArgumentException 사용자 없음
     */
    @Transactional
    public void updateNickname(Long memberId, String newNickname) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (m.getNickname().equals(newNickname)) {
            throw new IllegalArgumentException("기존 닉네임과 동일한 값으로는 변경할 수 없습니다");
        }

        m.setNickname(newNickname);
    }

    /**
     * 비밀번호 찾기 : 비밀번호 재설정
     * @param req 비밀번호 재설정 요청 DTO (이메일, 새 비밀번호, 확인 비밀번호 포함)
     * @throws  IllegalArgumentException 인증 실패, 회원 미존재, 비밀번호 불일치 등의 경우 발생
     */
    @Transactional
    public void resetPassword(PasswordResetRequestDto req) {
        String email = req.getEmail().trim().toLowerCase();

        if (!emailVerificationService.isVerified(email)) {
            throw new UnauthorizedException("이메일 인증이 필요합니다.");
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
        }

        member.setPassword(passwordEncoder.encode(req.getNewPassword()));
        emailVerificationService.invalidateVerified(email);
    }

    /**
     * 현재 비밀번호 확인
     *
     * @param memberId  비밀번호를 확인할 회원 ID
     * @param currentPassword 사용자가 입력한 현재 비밀번호
     * @throws IllegalArgumentException 회원이 존재하지 않거나 비밀번호가 일치하지 않는 경우 발생
     */
    @Transactional(readOnly = true)
    public void confirmCurrentPassword(Long memberId, String currentPassword) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        if (!passwordEncoder.matches(currentPassword, m.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        redis.opsForValue().set(okKey(memberId), "true", TTL);
    }

    /**
     * 새 비밀번호 변경
     *
     * @param memberId  비밀번호를 변경할 회원 ID
     * @param newPassword 새 비밀번호
     * @param confirmPassword 확인용 비밀번호
     * @throws UnauthorizedException 현재 비밀번호 확인 절차를 거치지 않은 경우 발생
     * @throws IllegalArgumentException 회원이 존재하지 않거나, 새 비밀번호 검증에 실패한 경우 발생
     */
    @Transactional
    public void changePassword(Long memberId, String newPassword, String confirmPassword) {
        String flag = redis.opsForValue().get(okKey(memberId));
        if (!"true".equals(flag)) {
            throw new UnauthorizedException("비밀번호 변경을 위해서는 먼저 현재 비밀번호 확인이 필요합니다.");
        }

        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(newPassword, m.getPassword())) {
            throw new IllegalArgumentException("이전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
        }

        m.setPassword(passwordEncoder.encode(newPassword));
        redis.delete(okKey(memberId)); // 재사용 방지
    }

    /**
     *
     * @param keyword
     * @param excludeId
     * @return
     */

    public List<MemberSearchResponseDto> searchMembers(String keyword, Long excludeId) {
        return memberRepository.searchByLoginId(keyword, excludeId)
                .stream()
                .map(m -> new MemberSearchResponseDto(
                        m.getId(),
                        m.getLoginId()
                ))
                .toList();
    }


    public long countMembersWithLoginId() {
        return memberRepository.countMembersWithLoginId();
    }

}
