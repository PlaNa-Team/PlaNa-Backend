# PlaNa 백엔드 프로젝트 종합 분석서

## 📋 프로젝트 개요

PlaNa (Plan + Diary) 백엔드는 일정 관리와 다이어리 작성을 통합한 개인용 캘린더 플랫폼의 서버 애플리케이션입니다. **신입 개발자의 포트폴리오 프로젝트**로서, 현대적인 웹 애플리케이션 개발의 핵심 요소들을 체계적으로 구현한 프로젝트입니다.

### 기술 스택 요약
- **Backend Framework**: Spring Boot 3.4.5 (Java 17)
- **Database**: MySQL/MariaDB (Production), H2 (Test)
- **Security**: Spring Security 6 + JWT + OAuth2
- **Real-time Communication**: WebSocket + STOMP
- **Build Tool**: Maven
- **Architecture**: Layered Architecture + Domain-Driven Design 요소

## 🏗 프로젝트 구조 분석

### 패키지 구조
```
com.plana/
├── auth/           # 인증/인가 도메인
├── calendar/       # 일정 관리 도메인
├── diary/          # 다이어리 도메인
├── notification/   # 실시간 알림 도메인
├── journal/        # 저널 도메인
├── project/        # 프로젝트 관리 도메인
└── PlaNaApplication.java
```

### 레이어드 아키텍처 구현
각 도메인은 일관된 레이어 구조를 따릅니다:

```
domain/
├── controller/     # REST API 엔드포인트
├── service/        # 비즈니스 로직 (인터페이스 + 구현체)
├── repository/     # 데이터 액세스 계층
├── entity/         # JPA 엔티티
├── dto/           # 요청/응답 객체
│   ├── request/
│   └── response/
└── enums/         # 도메인별 열거형
```

## 🎯 도메인별 핵심 기능 분석

### 1. Auth 도메인 - 인증/인가 시스템

#### 🔒 **보안 아키텍처의 우수함**

**Member 엔티티 설계**
```java
@Entity
@Table(name = "member")
@Where(clause = "is_deleted = false") // Soft Delete 패턴
public class Member {
    @Column(nullable = false, unique = true, length = 100)
    private String email; // 통합 로그인 ID

    @Enumerated(EnumType.STRING)
    private SocialProvider provider; // 다중 소셜 로그인 지원

    @Builder.Default
    private String role = "ROLE_USER"; // 권한 관리
}
```

**학습 포인트:**
- **Soft Delete 패턴**: 물리적 삭제 대신 논리적 삭제로 데이터 무결성 보장
- **통합 로그인**: 일반 로그인과 소셜 로그인을 하나의 엔티티로 통합 처리
- **Enum 활용**: SocialProvider로 확장 가능한 소셜 로그인 지원

#### 🛡 **JWT + OAuth2 통합 인증**

**SecurityConfig 핵심 설계**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 무상태 JWT
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class) // JWT 필터 체인
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)) // OAuth2 통합
            .build();
    }
}
```

**우수한 설계 포인트:**
1. **Stateless 아키텍처**: 세션 없는 JWT 기반 인증으로 확장성 확보
2. **필터 체인 최적화**: 적절한 순서로 보안 필터 배치
3. **OAuth2 팩토리 패턴**: 구글, 카카오, 네이버 등 다중 제공업체 지원

### 2. Calendar 도메인 - 복잡한 비즈니스 로직 처리

#### 📅 **iCal4j 기반 반복 일정 시스템**

**Schedule 엔티티의 정교한 설계**
```java
@Entity
public class Schedule {
    @Column(length = 255)
    private String recurrenceRule; // RFC 5545 RRule 표준

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
    private List<ScheduleAlarm> alarms = new ArrayList<>(); // 연관관계 관리

    @PrePersist @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(); // 자동 타임스탬프
    }
}
```

**RecurrenceService의 혁신적 구현**
```java
@Service
public class RecurrenceServiceImpl implements RecurrenceService {
    public List<LocalDateTime> generateInstancesInRange(String rrule,
                                                       LocalDateTime scheduleStartAt,
                                                       LocalDateTime rangeStart,
                                                       LocalDateTime rangeEnd) {
        // iCal4j 라이브러리 활용으로 국제 표준 준수
        return RRuleUtils.generateRecurrenceInstances(
            rrule, scheduleStartAt, rangeStart, rangeEnd, 100);
    }
}
```

**Virtual ID 패턴의 창의적 적용**
```java
// 반복 일정의 각 인스턴스에 고유 ID 부여
String virtualId = String.format("recurring-%d-%d",
    recurringSchedule.getId(),
    instanceStart.toEpochSecond(ZoneOffset.UTC));
```

**학습 포인트:**
- **외부 라이브러리 활용**: iCal4j로 복잡한 반복 규칙을 표준 준수하여 구현
- **동적 데이터 생성**: DB에 저장하지 않고 조회 시점에 반복 인스턴스 생성
- **성능 최적화**: 조회 범위 제한 및 인스턴스 수 제한으로 성능 보장

### 3. Notification 도메인 - 실시간 통신 시스템

#### 🔄 **WebSocket + STOMP 실시간 알림**

**WebSocket 설정의 체계적 구현**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/user"); // 브로커 설정
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user"); // 개인별 메시지 라우팅
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws")
                .addInterceptors(jwtHandshakeInterceptor) // JWT 인증 통합
                .setAllowedOriginPatterns("*");
    }
}
```

**멀티 세션 관리의 우수한 구현**
```java
@Service
public class WebSocketSessionManager {
    // 한 사용자가 여러 디바이스/탭에서 접속 지원
    private final Map<Long, Set<String>> memberSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionMembers = new ConcurrentHashMap<>();

    public boolean isUserOnline(Long memberId) {
        return memberSessions.containsKey(memberId) &&
               !memberSessions.get(memberId).isEmpty();
    }
}
```

**실시간 알림 발송 로직**
```java
public void sendRealTimeNotification(Notification notification) {
    Long memberId = notification.getMember().getId();

    if (sessionManager.isUserOnline(memberId)) {
        String directDestination = "/user/" + memberId + "/queue/notifications";
        messagingTemplate.convertAndSend(directDestination, responseDto);
        log.info("실시간 알림 발송 완료: memberId={}", memberId);
    }

    // 온라인/오프라인 상관없이 DB 저장 (영속성 보장)
    notification.setIsSent(true);
    notificationRepository.save(notification);
}
```

**학습 포인트:**
- **STOMP 프로토콜**: 표준 메시징 프로토콜로 안정적인 실시간 통신
- **JWT 통합 인증**: WebSocket HandshakeInterceptor로 기존 인증과 일관성 유지
- **멀티 디바이스 지원**: 현대적인 사용자 경험을 위한 다중 세션 관리

### 4. Diary 도메인 - 소셜 기능과 다형성

#### 📖 **다이어리 타입별 처리와 태그 시스템**

**DiaryTag의 사회적 기능 구현**
```java
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"diary_id", "member_id"})
})
public class DiaryTag {
    @Enumerated(EnumType.STRING)
    private TagStatus tagStatus; // 작성자, 미설정, 수락, 거절, 삭제

    @Column(length = 100)
    private String tagText; // 비회원 태그도 지원
}
```

**Template Method 패턴 적용**
```java
@Transactional
public DiaryCreateResponseDto createDiary(DiaryCreateRequestDto request, Long writerId) {
    // 1. 공통 처리
    Diary diary = createBaseDiary(request, writerId);

    // 2. 타입별 특화 처리
    switch (request.getDiaryType()) {
        case DAILY -> processDailyDiary(diary, request.getDailyContent());
        case BOOK -> processBookDiary(diary, request.getBookContent());
        case MOVIE -> processMovieDiary(diary, request.getMovieContent());
    }

    // 3. 공통 후처리 (알림 발송)
    processTagNotifications(savedDiary, request.getTagList());
    return buildResponse(savedDiary);
}
```

## 🔍 아키텍처 설계 품질 평가

### ✅ **우수한 설계 사례**

#### 1. **객체지향 설계 원칙 준수**
```java
// 단일 책임 원칙: 각 Service가 명확한 역할
public interface CalendarService { /* 일정 관리 */ }
public interface RecurrenceService { /* 반복 규칙 처리 */ }
public interface NotificationService { /* 알림 처리 */ }

// 의존성 주입: 인터페이스 기반 느슨한 결합
@Service
public class CalendarServiceImpl implements CalendarService {
    private final ScheduleRepository scheduleRepository;
    private final NotificationService notificationService; // 인터페이스 의존
}
```

#### 2. **도메인 중심 모듈화**
- 각 도메인이 독립적인 패키지 구조
- 도메인별 명확한 책임 분리
- 확장 시 기존 코드 영향 최소화

#### 3. **현대적 기술 스택 활용**
- Spring Boot 3.x 최신 기능 활용
- JWT 보안과 OAuth2 소셜 로그인 통합
- WebSocket 실시간 통신
- Docker 컨테이너화 및 프로덕션 배포

### ⚠️ **개선이 필요한 설계**

#### 1. **도메인 간 직접 의존성**
```java
// 현재: notification 도메인이 다른 도메인을 직접 참조
@Service
public class NotificationServiceImpl {
    private final DiaryTagRepository diaryTagRepository; // diary 도메인 의존
    private final ScheduleAlarmRepository scheduleAlarmRepository; // calendar 도메인 의존
}
```

**문제점:**
- 도메인 간 강한 결합도
- 순환 의존성 위험
- 테스트 어려움

**개선 방향:**
```java
// 이벤트 기반 통신으로 개선
@EventListener
public void handleDiaryTagEvent(DiaryTagCreatedEvent event) {
    createDiaryTagNotification(event);
}
```

#### 2. **다형성 구조 미활용**
```java
// 현재: 별도 엔티티로 분리
@Entity public class Diary { /* 기본 다이어리 */ }
@Entity public class Daily { /* Daily 전용 */ }
@Entity public class Book { /* Book 전용 */ }
```

**개선 방향:**
```java
// JPA 상속 매핑 활용
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "diary_type")
public abstract class Diary { /* 공통 속성 */ }

@Entity
@DiscriminatorValue("DAILY")
public class DailyDiary extends Diary { /* Daily 특화 속성 */ }
```

#### 3. **설정 하드코딩**
```java
// 현재: 코드에 하드코딩된 상수들
List<LocalDateTime> instances = RRuleUtils.generateRecurrenceInstances(
    rrule, scheduleStartAt, rangeStart, rangeEnd, 100); // 하드코딩
```

**개선 방향:**
```yaml
# application.yml
calendar:
  recurrence:
    max-instances: 100
    default-range-days: 365
```

## 📊 코드 품질 메트릭

### 응집도 (Cohesion) - **높음**
- 각 도메인 내 관련 기능들이 잘 그룹화
- 복잡한 비즈니스 로직의 적절한 캡슐화
- Service 계층의 명확한 책임 분담

### 결합도 (Coupling) - **중간**
- 도메인 간 일부 직접 의존성 존재
- 인터페이스 기반 설계로 구현체 간 결합도는 낮음
- 이벤트 기반 통신 도입으로 추가 개선 가능

### 확장성 (Scalability) - **높음**
- OAuth2 팩토리 패턴으로 새로운 소셜 제공업체 추가 용이
- 반복 일정 알고리즘 교체 가능한 인터페이스 설계
- WebSocket 세션 관리로 멀티 디바이스 지원

### 유지보수성 (Maintainability) - **중상**
- 일관된 패키지 구조와 명명 규칙
- 충분한 주석과 문서화
- 단위 테스트 부족으로 개선 필요

## 🚀 성능 최적화 사례

### 1. **지연 로딩과 Fetch Join**
```java
// N+1 문제 방지
@Query("SELECT s FROM Schedule s JOIN FETCH s.category WHERE s.member.id = :memberId")
List<Schedule> findSchedulesWithCategory(@Param("memberId") Long memberId);
```

### 2. **트랜잭션 최적화**
```java
@Transactional(readOnly = true) // 기본값으로 읽기 최적화
public class CalendarServiceImpl {

    @Transactional // 쓰기 작업시에만 Override
    public ScheduleCreateResponseDto createSchedule(...) {
        // 최소한의 트랜잭션 범위
    }
}
```

### 3. **WebSocket 연결 관리**
```java
// 동시성 안전한 세션 관리
private final Map<String, Long> sessionToMemberMap = new ConcurrentHashMap<>();
private final Map<Long, Set<String>> memberToSessionsMap = new ConcurrentHashMap<>();
```

## 🎓 신입 개발자 학습 가이드

### 필수 학습 영역

#### 1. **Spring Boot 생태계**
- **의존성 주입**: `@Autowired`, `@RequiredArgsConstructor` 활용
- **AOP**: `@Transactional`, `@PrePersist` 등 횡단 관심사
- **Configuration**: `@Configuration`, `@Bean` 설정 관리

#### 2. **JPA/Hibernate**
- **엔티티 매핑**: `@Entity`, `@Table`, 연관관계 매핑
- **생명주기 훅**: `@PrePersist`, `@PreUpdate` 활용
- **쿼리 최적화**: JPQL, 지연/즉시 로딩

#### 3. **보안 프로그래밍**
- **Spring Security**: 필터 체인, 인증/인가 처리
- **JWT**: 토큰 기반 인증의 장단점과 구현
- **OAuth2**: 소셜 로그인 표준과 보안 고려사항

#### 4. **실시간 통신**
- **WebSocket**: 양방향 통신 프로토콜
- **STOMP**: 메시징 표준과 메시지 브로커
- **세션 관리**: 멀티 디바이스 환경에서의 세션 추적

### 실습 프로젝트 제안

#### 기초 단계
1. Member 엔티티 CRUD API 구현
2. JWT 인증 필터 직접 구현
3. 단순 WebSocket Echo 서버 구축

#### 중급 단계
1. 반복 일정 알고리즘 직접 구현
2. 이벤트 기반 알림 시스템 설계
3. 다형성을 활용한 다이어리 타입 확장

#### 고급 단계
1. 마이크로서비스 아키텍처로 분리
2. 캐싱 전략 수립 및 적용
3. 성능 테스트 및 병목점 분석

## 🎯 결론: 프로젝트 평가

PlaNa 백엔드 프로젝트는 **신입 개발자 포트폴리오로서 매우 우수한 품질**을 보여줍니다:

### 🌟 **특히 인상적인 부분**
1. **복잡한 비즈니스 로직 구현**: iCal4j 기반 반복 일정 처리
2. **현대적 기술 스택**: Spring Boot 3.x, JWT, WebSocket 통합
3. **프로덕션 배포 경험**: Docker, nginx, HTTPS 환경 구축
4. **실시간 기능**: WebSocket 기반 멀티 디바이스 알림 시스템

### 📈 **성장 가능성**
현재 구조는 다음과 같은 확장이 가능합니다:
- 마이크로서비스 아키텍처로의 전환
- 이벤트 소싱 패턴 적용
- CQRS를 통한 읽기/쓰기 분리
- Kafka를 활용한 대용량 메시지 처리

이 프로젝트는 **단순한 CRUD를 넘어선 실제 서비스 수준의 복잡성**을 다루고 있으며, **신입 개발자가 실무에서 마주할 다양한 기술적 도전**을 적절히 해결한 우수한 학습 사례입니다.

---

**작성일**: 2025-09-30
**분석 대상**: PlaNa Backend v0.0.1-SNAPSHOT
**분석 범위**: 전체 도메인 (auth, calendar, diary, notification, journal, project)