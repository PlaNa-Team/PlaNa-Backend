package com.plana.auth.service;

import com.plana.auth.dto.LoginRequestDto;
import com.plana.auth.dto.LoginResponseDto;
import com.plana.auth.dto.SignupRequestDto;
import com.plana.auth.dto.SignupResponseDto;
import com.plana.auth.entity.User;
import com.plana.auth.enums.SocialProvider;
import com.plana.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 일반 회원가입 처리
     * @param signupRequest 회원가입 요청 정보
     * @return 회원가입 응답 정보
     * @throws IllegalArgumentException 이메일 중복, 비밀번호 불일치 등
     */
    @Transactional
    public SignupResponseDto signup(SignupRequestDto signupRequest) {
        log.info("일반 회원가입 시도: {}", signupRequest.getEmail());
        
        // 1. 비밀번호 일치 검증
        if (!signupRequest.isPasswordMatch()) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }
        
        // 2. 이메일 중복 검증 (소셜 로그인 포함)
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다");
        }
        
        // 3. 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(signupRequest.getPassword());
        
        // 4. User 엔티티 생성
        User newUser = User.builder()
                .email(signupRequest.getEmail())
                .name(signupRequest.getName())
                .password(encryptedPassword) // 암호화된 비밀번호
                .provider(SocialProvider.LOCAL) // 일반 로그인 구분
                .providerId(null) // 일반 로그인은 providerId 없음
                .role("ROLE_USER") // 기본 권한
                .enabled(true) // 계정 활성화
                .build();
        
        // 5. 데이터베이스 저장
        User savedUser = userRepository.save(newUser);
        
        log.info("일반 회원가입 완료: userId={}, email={}", savedUser.getId(), savedUser.getEmail());
        
        // 6. 응답 DTO 생성
        return SignupResponseDto.builder()
                .message("회원가입이 완료되었습니다")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
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
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));
        
        // 2. 일반 로그인 사용자인지 확인 (소셜 로그인 사용자는 password가 null)
        if (user.getProvider() != SocialProvider.LOCAL || user.getPassword() == null) {
            throw new IllegalArgumentException("소셜 로그인으로 가입된 계정입니다. 소셜 로그인을 사용해주세요");
        }
        
        // 3. 계정 활성화 상태 확인
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("비활성화된 계정입니다. 관리자에게 문의하세요");
        }
        
        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        
        // 5. JWT 토큰 생성 (기존 소셜 로그인과 동일한 방식)
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
        
        log.info("일반 로그인 성공: userId={}, email={}", user.getId(), user.getEmail());
        
        // 6. 응답 DTO 생성 (소셜 로그인과 동일한 구조)
        LoginResponseDto.UserInfoDto userInfo = LoginResponseDto.UserInfoDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider().getValue())
                .build();
        
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .expiresIn(3600L) // 1시간
                .user(userInfo)
                .build();
    }

    /**
     * 이메일 중복 검사
     * @param email 검사할 이메일
     * @return 중복이면 true, 사용 가능하면 false
     */
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 사용자 ID로 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     * @throws IllegalArgumentException 사용자 없음
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }

    /**
     * 이메일로 사용자 정보 조회
     * @param email 사용자 이메일
     * @return 사용자 정보
     * @throws IllegalArgumentException 사용자 없음
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }
}
