package com.plana.auth.controller;

import com.plana.auth.entity.Member;
import com.plana.auth.enums.SocialProvider;
import com.plana.auth.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 * Phase 2.1: 기본 통합 테스트 환경 설정 및 첫 번째 테스트
 * 
 * 통합 테스트 특징:
 * - @SpringBootTest: 전체 Spring Context 로드
 * - MockMvc: 수동 설정으로 확실한 동작 보장
 * - 실제 HTTP 요청/응답 시뮬레이션
 * - H2 메모리 DB 사용으로 실제 DB 연동 테스트
 * - @Transactional: 테스트 후 데이터베이스 롤백으로 테스트 격리
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional  // 테스트 후 데이터베이스 롤백
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private MemberRepository memberRepository;  // 데이터베이스 검증용
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        // MockMvc 수동 설정 - 가장 확실한 방법
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }
    
    @Test
    @DisplayName("GET /api/auth/status - 서버 상태 확인 API 테스트")
    void getStatus_Success() throws Exception {
        // Given - 특별한 준비 없음 (상태 확인 API)
        
        // When & Then - HTTP GET 요청 및 응답 검증
        mockMvc.perform(get("/api/auth/status"))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isOk()) // HTTP 200 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.message").value("Authentication server is running")) // 메시지 확인
                .andExpect(jsonPath("$.timestamp").exists()); // 타임스탬프 존재 확인
    }
    
    @Test
    @DisplayName("POST /api/auth/signup - 정상 회원가입 성공")
    void signup_ValidRequest_Success() throws Exception {
        // Given - 유효한 회원가입 요청 데이터
        String signupJson = """
            {
                "email": "test@example.com",
                "password": "password123",
                "passwordConfirm": "password123",
                "name": "테스터"
            }
            """;
        
        // When & Then - HTTP POST 요청 및 응답 검증
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isCreated()) // HTTP 201 Created 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다")) // 성공 메시지 확인
                .andExpect(jsonPath("$.memberId").exists()) // 사용자 ID 존재 확인
                .andExpect(jsonPath("$.email").value("test@example.com")) // 이메일 확인
                .andExpect(jsonPath("$.name").value("테스터")); // 이름 확인
        
        // 데이터베이스에 실제로 저장되었는지 검증
        var savedUser = memberRepository.findByEmail("test@example.com");
        assert savedUser.isPresent() : "사용자가 데이터베이스에 저장되지 않았습니다";
        assert savedUser.get().getName().equals("테스터") : "저장된 사용자 이름이 일치하지 않습니다";
    }
    
    @Test
    @DisplayName("POST /api/auth/signup - 이메일 중복시 회원가입 실패")
    void signup_DuplicateEmail_Failure() throws Exception {
        // Given - 기존 사용자 데이터 준비 (첫 번째 회원가입)
        String firstSignupJson = """
            {
                "email": "duplicate@example.com",
                "password": "password123",
                "passwordConfirm": "password123",
                "name": "첫번째사용자"
            }
            """;
        
        System.out.println("=== 첫 번째 회원가입 시도 시작 ===");
        
        // 첫 번째 회원가입 성공 - 상세 로그 추가
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstSignupJson))
                .andDo(print()) // 첫 번째 요청 상세 로그
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"));
        
        System.out.println("=== 첫 번째 회원가입 성공! 이제 두 번째 시도 ===");
        
        // When & Then - 동일한 이메일로 두 번째 회원가입 시도
        String secondSignupJson = """
            {
                "email": "duplicate@example.com",
                "password": "different123",
                "passwordConfirm": "different123",
                "name": "두번째사용자"
            }
            """;
        
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondSignupJson))
                .andDo(print()) // 두 번째 요청 상세 로그
                .andExpect(status().isBadRequest()) // HTTP 400 Bad Request 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.message").value("이미 사용중인 이메일입니다")); // 오류 메시지 확인
        
        // 데이터베이스에는 첫 번째 사용자만 존재하는지 검증
        var members = memberRepository.findAll();
        var duplicateEmailUsers = members.stream()
                .filter(member -> "duplicate@example.com".equals(member.getEmail()))
                .toList();
        assert duplicateEmailUsers.size() == 1 : "중복 이메일로 여러 사용자가 생성되었습니다";
        assert duplicateEmailUsers.get(0).getName().equals("첫번째사용자") : "잘못된 사용자가 저장되었습니다";
        
        System.out.println("=== 중복 테스트 완료 ===");
    }
    
    @Test
    @DisplayName("POST /api/auth/signup - 비밀번호 불일치시 회원가입 실패")
    void signup_PasswordMismatch_Failure() throws Exception {
        // Given - 비밀번호와 비밀번호 확인이 다른 요청 데이터
        String signupJson = """
            {
                "email": "mismatch@example.com",
                "password": "password123",
                "passwordConfirm": "different123",
                "name": "비밀번호불일치사용자"
            }
            """;
        
        // When & Then - HTTP POST 요청 및 응답 검증
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isBadRequest()) // HTTP 400 Bad Request 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다")); // 오류 메시지 확인
        
        // 데이터베이스에 사용자가 저장되지 않았는지 검증
        var savedUser = memberRepository.findByEmail("mismatch@example.com");
        assert savedUser.isEmpty() : "비밀번호 불일치 오류 시 사용자가 저장되었습니다";
    }
    
    @Test
    @DisplayName("POST /api/auth/signup - 잘못된 이메일 형식으로 회원가입 실패")
    void signup_InvalidEmailFormat_Failure() throws Exception {
        // Given - 잘못된 이메일 형식을 가진 요청 데이터
        String signupJson = """
            {
                "email": "invalid-email-format",
                "password": "password123",
                "passwordConfirm": "password123",
                "name": "잘못된이메일사용자"
            }
            """;
        
        // When & Then - HTTP POST 요청 및 응답 검증
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isBadRequest()) // HTTP 400 Bad Request 상태 코드 확인
                .andExpect(content().contentType("application/json")); // JSON 응답 확인
                // Bean Validation 오류 메시지는 세부적이라 일단 생략
        
        // 데이터베이스에 사용자가 저장되지 않았는지 검증
        var savedUser = memberRepository.findByEmail("invalid-email-format");
        assert savedUser.isEmpty() : "잘못된 이메일 형식 오류 시 사용자가 저장되었습니다";
    }
    
    // ================================
    // POST /api/auth/login 테스트들
    // ================================
    
    @Test
    @DisplayName("POST /api/auth/login - 정상 로그인 성공")
    void login_ValidCredentials_Success() throws Exception {
        // Given - 먼저 회원가입으로 사용자 생성
        String signupJson = """
            {
                "email": "logintest@example.com",
                "password": "password123",
                "passwordConfirm": "password123",
                "name": "로그인테스터"
            }
            """;
        
        System.out.println("=== 로그인 테스트를 위한 회원가입 먼저 진행 ===");
        
        // 회원가입 먼저 수행
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isCreated());
        
        System.out.println("=== 회원가입 완료, 이제 로그인 시도 ===");
        
        // When & Then - 로그인 요청
        String loginJson = """
            {
                "email": "logintest@example.com",
                "password": "password123"
            }
            """;
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isOk()) // HTTP 200 OK 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.accessToken").exists()) // JWT 토큰 존재 확인
                .andExpect(jsonPath("$.accessToken").isString()) // JWT 토큰이 문자열인지 확인
                .andExpect(jsonPath("$.expiresIn").value(3600)) // 만료 시간 확인
                .andExpect(jsonPath("$.member").exists()) // 사용자 정보 존재 확인
                .andExpect(jsonPath("$.member.id").exists()) // 사용자 ID 존재 확인
                .andExpect(jsonPath("$.member.email").value("logintest@example.com")) // 이메일 확인
                .andExpect(jsonPath("$.member.name").value("로그인테스터")) // 이름 확인
                .andExpect(jsonPath("$.member.provider").value("local")) // 프로바이더 확인 (소문자)
                .andExpect(jsonPath("$.timestamp").exists()); // 타임스탬프 존재 확인
        
        System.out.println("=== 로그인 성공 테스트 완료 ===");
    }
    
    @Test
    @DisplayName("POST /api/auth/login - 존재하지 않는 이메일로 로그인 실패")
    void login_NonexistentEmail_Failure() throws Exception {
        // Given - 존재하지 않는 이메일로 로그인 시도
        String loginJson = """
            {
                "email": "nonexistent@example.com",
                "password": "password123"
            }
            """;
        
        // When & Then - HTTP POST 요청 및 응답 검증
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다")); // 오류 메시지 확인
    }
    
    @Test
    @DisplayName("POST /api/auth/login - 비밀번호 불일치로 로그인 실패")
    void login_WrongPassword_Failure() throws Exception {
        // Given - 먼저 회원가입으로 사용자 생성
        String signupJson = """
            {
                "email": "wrongpassword@example.com",
                "password": "correctpassword123",
                "passwordConfirm": "correctpassword123",
                "name": "비밀번호테스터"
            }
            """;
        
        // 회원가입 먼저 수행
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isCreated());
        
        // When & Then - 잘못된 비밀번호로 로그인 시도
        String loginJson = """
            {
                "email": "wrongpassword@example.com",
                "password": "wrongpassword123"
            }
            """;
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다")); // 오류 메시지 확인
    }
    
    @Test
    @DisplayName("POST /api/auth/login - 소셜 계정으로 일반 로그인 시도 실패")
    void login_SocialAccountAttempt_Failure() throws Exception {
        // Given - 소셜 계정 사용자를 직접 데이터베이스에 생성
        // 실제로는 OAuth2를 통해 생성되지만, 테스트에서는 직접 생성
        var socialMember = Member.builder()
                .email("social@example.com")
                .name("소셜사용자")
                .provider(SocialProvider.GOOGLE) // 구글 소셜 로그인
                .providerId("google123456789")
                .password(null) // 소셜 로그인은 비밀번호 없음
                .role("ROLE_USER")
                .enabled(true)
                .build();
        
        memberRepository.save(socialMember);
        
        System.out.println("=== 소셜 계정 생성 완료, 일반 로그인 시도 ===");
        
        // When & Then - 소셜 계정으로 일반 로그인 시도
        String loginJson = """
            {
                "email": "social@example.com",
                "password": "anypassword123"
            }
            """;
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andDo(print()) // 요청/응답 내용 콘솔 출력
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized 상태 코드 확인
                .andExpect(content().contentType("application/json")) // JSON 응답 확인
                .andExpect(jsonPath("$.message").value("소셜 로그인으로 가입된 계정입니다. 소셜 로그인을 사용해주세요")); // 오류 메시지 확인
        
        System.out.println("=== 소셜 계정 일반 로그인 차단 테스트 완료 ===");
    }
}
