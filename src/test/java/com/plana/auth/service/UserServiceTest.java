package com.plana.auth.service;

import com.plana.auth.dto.LoginRequestDto;
import com.plana.auth.dto.LoginResponseDto;
import com.plana.auth.dto.SignupRequestDto;
import com.plana.auth.dto.SignupResponseDto;
import com.plana.auth.entity.User;
import com.plana.auth.enums.SocialProvider;
import com.plana.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * UserService 단위 테스트
 * Phase 1.1: 이미 구현된 signup()/login() 메서드 검증
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    // 🎭 가짜 객체들 (Mock)
    // @Mock: "가짜로 만들어줘" → 실제 DB 접속 안함, 실제 암호화 안함
    @Mock
    private UserRepository userRepository;      // 가짜 DB
    
    @Mock
    private PasswordEncoder passwordEncoder;    // 가짜 암호화기
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;  // 가짜 토큰생성기

    // 🎯 진짜 객체 (실제 테스트 대상)
    // @InjectMocks: "진짜 UserService 만들되, 의존성은 위의 가짜들로 채워줘"
    @InjectMocks
    private UserService userService;            // 진짜 UserService
    
    private SignupRequestDto validSignupRequest;
    private LoginRequestDto validLoginRequest;
    private User savedUser;
    
    @BeforeEach // 각 테스트 전 준비작업, 매 테스트마다 실행됨! 테스트용 데이터 준비
    void setUp() {
        // 테스트용 유효한 회원가입 요청 데이터
        validSignupRequest = SignupRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("테스터")
                .build();
        
        // 테스트용 유효한 로그인 요청 데이터
        validLoginRequest = LoginRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();
        
        // 테스트용 저장된 사용자 데이터
        savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스터")
                .password("encryptedPassword")
                .provider(SocialProvider.LOCAL)
                .providerId(null)
                .role("ROLE_USER")
                .enabled(true)
                .build();
    }
    
    @Test
    @DisplayName("정상적인 회원가입 요청시 성공 - BCrypt 암호화 검증 포함")
    void signup_ValidRequest_Success() {
        // ===== Given (준비) =====
        // Given - Phase 1.1 계획: 정상 회원가입 성공 테스트
        given(userRepository.existsByEmail(validSignupRequest.getEmail())).willReturn(false);
        given(passwordEncoder.encode(validSignupRequest.getPassword())).willReturn("encryptedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // ===== When (실행) =====
        // When - 이미 구현된 signup() 메서드 호출
        SignupResponseDto result = userService.signup(validSignupRequest);

        // ===== Then (검증) =====
        // Then - 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("회원가입이 완료되었습니다");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("테스터");
        
        // Mock 호출 검증
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getProvider() == SocialProvider.LOCAL &&
            user.getPassword().equals("encryptedPassword") &&
            user.getRole().equals("ROLE_USER") &&
            user.getEnabled().equals(true)
        ));
    }
    
    @Test
    @DisplayName("이메일 중복시 회원가입 실패 - 소셜 로그인 포함 검증")
    void signup_DuplicateEmail_ThrowsException() {
        // Given - Phase 1.1 계획: 이메일 중복 실패 테스트
        given(userRepository.existsByEmail(validSignupRequest.getEmail())).willReturn(true);
        
        // When & Then - 이미 구현된 예외 처리 검증
        assertThatThrownBy(() -> userService.signup(validSignupRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용중인 이메일입니다");
        
        // 비밀번호 암호화가 호출되지 않았는지 확인
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("비밀번호 불일치시 회원가입 실패")
    void signup_PasswordMismatch_ThrowsException() {
        // Given - Phase 1.1 계획: 비밀번호 불일치 실패 테스트
        SignupRequestDto mismatchRequest = SignupRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .passwordConfirm("differentPassword")
                .name("테스터")
                .build();
        
        // When & Then - 이미 구현된 비밀번호 검증 로직 확인
        assertThatThrownBy(() -> userService.signup(mismatchRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다");
        
        // 다른 검증 로직이 호출되지 않았는지 확인
        verify(userRepository, never()).existsByEmail(any());
        verify(passwordEncoder, never()).encode(any());
    }
    
    @Test
    @DisplayName("정상 로그인 성공 - JWT 토큰 생성 포함")
    void login_ValidCredentials_Success() {
        // Given - Phase 1.1 계획: 정상 로그인 성공 테스트
        given(userRepository.findByEmail(validLoginRequest.getEmail())).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches(validLoginRequest.getPassword(), savedUser.getPassword())).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, "test@example.com", "ROLE_USER")).willReturn("test.jwt.token");
        
        // When - 이미 구현된 login() 메서드 호출
        LoginResponseDto result = userService.login(validLoginRequest);
        
        // Then - 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("test.jwt.token");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
        assertThat(result.getUser().getId()).isEqualTo(1L);
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");
        
        assertThat(result.getUser().getProvider()).isEqualTo("local");
        // Mock 호출 검증
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encryptedPassword");
        verify(jwtTokenProvider).createAccessToken(1L, "test@example.com", "ROLE_USER");
    }
    
    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 실패")
    void login_UserNotFound_ThrowsException() {
        // Given - Phase 1.1 계획: 존재하지 않는 이메일 테스트
        given(userRepository.findByEmail(validLoginRequest.getEmail())).willReturn(Optional.empty());
        
        // When & Then - 이미 구현된 예외 처리 검증
        assertThatThrownBy(() -> userService.login(validLoginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
        
        // 비밀번호 검증이 호출되지 않았는지 확인
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
    }
    
    @Test
    @DisplayName("비밀번호 불일치로 로그인 실패")
    void login_WrongPassword_ThrowsException() {
        // Given - Phase 1.1 계획: 비밀번호 불일치 테스트
        given(userRepository.findByEmail(validLoginRequest.getEmail())).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches(validLoginRequest.getPassword(), savedUser.getPassword())).willReturn(false);
        
        // When & Then - 이미 구현된 예외 처리 검증
        assertThatThrownBy(() -> userService.login(validLoginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
        
        // JWT 토큰 생성이 호출되지 않았는지 확인
        verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
    }
    
    @Test
    @DisplayName("소셜 계정으로 일반 로그인 시도시 실패 - 중요!")
    void login_SocialAccountAttempt_ThrowsException() {
        // Given - Phase 1.1 계획: 소셜 계정으로 일반 로그인 시도 테스트
        User socialUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("구글사용자")
                .password(null)  // 소셜 로그인은 비밀번호가 null
                .provider(SocialProvider.GOOGLE)
                .providerId("google123")
                .role("ROLE_USER")
                .enabled(true)
                .build();
        
        given(userRepository.findByEmail(validLoginRequest.getEmail())).willReturn(Optional.of(socialUser));
        
        // When & Then - 이미 구현된 소셜/일반 계정 구분 로직 검증
        assertThatThrownBy(() -> userService.login(validLoginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인으로 가입된 계정입니다. 소셜 로그인을 사용해주세요");
        
        // 비밀번호 검증이 호출되지 않았는지 확인
        verify(passwordEncoder, never()).matches(any(), any());
    }
    
    @Test
    @DisplayName("비활성화 계정으로 로그인 실패")
    void login_DisabledAccount_ThrowsException() {
        // Given - Phase 1.1 계획: 비활성화 계정 로그인 시도 테스트
        User disabledUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스터")
                .password("encryptedPassword")
                .provider(SocialProvider.LOCAL)
                .role("ROLE_USER")
                .enabled(false)  // 비활성화 상태
                .build();
        
        given(userRepository.findByEmail(validLoginRequest.getEmail())).willReturn(Optional.of(disabledUser));
        
        // When & Then - 이미 구현된 계정 상태 확인 로직 검증
        assertThatThrownBy(() -> userService.login(validLoginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비활성화된 계정입니다. 관리자에게 문의하세요");
        
        // 비밀번호 검증이 호출되지 않았는지 확인
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
