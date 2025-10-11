# PlaNa Backend 아키텍처 분석

## 📋 개요

PlaNa 백엔드 시스템의 아키텍처를 종합적으로 분석하고, 적용된 설계 패턴과 아키텍처 특징을 상세히 설명합니다.

## 🏛 전체 아키텍처 개요

PlaNa 프로젝트는 **레이어드 아키텍처(Layered Architecture)**를 기반으로 하며, **도메인 기반 모듈화(Domain-Based Modularization)**를 적용한 구조입니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  Controllers (REST API, WebSocket Message Handlers)        │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│     Services (Business Logic, Use Cases)                   │
├─────────────────────────────────────────────────────────────┤
│                    Infrastructure Layer                     │
│  Repositories, External Services, Configurations           │
├─────────────────────────────────────────────────────────────┤
│                       Domain Layer                          │
│     Entities, Enums, Domain Objects                        │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 적용된 아키텍처 패턴

### 1. **레이어드 아키텍처 (Layered Architecture)**

전형적인 Spring Boot 3-Layer 구조를 확장한 4-Layer 구조:

#### **Presentation Layer**
```java
// REST API Controllers
@RestController
@RequestMapping("/api/calendars")
public class CalendarController {
    private final CalendarService calendarService;
    // HTTP 요청 처리 및 응답 변환
}

// WebSocket Message Handlers
@MessageMapping("/connect")
public class NotificationController {
    // 실시간 메시지 처리
}
```

#### **Application Layer (Service Layer)**
```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {
    private final ScheduleRepository scheduleRepository;
    private final RecurrenceService recurrenceService;
    private final NotificationService notificationService;
    // 비즈니스 로직 조합 및 트랜잭션 관리
}
```

#### **Infrastructure Layer**
```java
// JPA Repositories
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    // 데이터 액세스 및 영속성 관리
}

// External Service Integrations
@Service
public class EmailSenderService {
    // 외부 서비스 연동
}
```

#### **Domain Layer**
```java
@Entity
public class Schedule {
    // 순수한 도메인 객체
    // 비즈니스 규칙과 제약사항 포함
}
```

### 2. **도메인 주도 설계 (Domain-Driven Design) 요소**

#### **도메인 모듈 분리**
```
com.plana/
├── auth/           # 인증/인가 도메인
├── calendar/       # 일정 관리 도메인
├── diary/          # 다이어리 도메인
├── notification/   # 알림 도메인
├── journal/        # 저널 도메인
├── project/        # 프로젝트 도메인
└── common/         # 공통 설정
```

#### **Bounded Context 구현**
각 도메인은 독립적인 컨텍스트를 가지며, 명확한 책임 분리:

- **Auth Domain**: 사용자 인증, OAuth2, JWT 처리
- **Calendar Domain**: 일정 CRUD, 반복 일정, 알림 설정
- **Diary Domain**: 다이어리 작성, 태그 시스템, 공유
- **Notification Domain**: 실시간 알림, WebSocket 통신

### 3. **MVC 패턴 (Model-View-Controller)**

Spring MVC 프레임워크 기반 구현:

```java
// Controller: HTTP 요청 처리
@GetMapping("/{year}/{month}")
public ResponseEntity<ApiResponse<ScheduleMonthlyResponseDto>> getMonthlySchedules(
    @PathVariable int year, @PathVariable int month,
    @AuthenticationPrincipal AuthenticatedMemberDto authMember) {

    // Model: Service 계층 호출
    List<ScheduleMonthlyItemDto> schedules =
        calendarService.getMonthlySchedules(authMember.getId(), year, month);

    // View: JSON 응답 반환
    return ResponseEntity.ok(ApiResponse.success("success", responseDto));
}
```

### 4. **의존성 역전 원칙 (Dependency Inversion Principle)**

Service Interface와 구현체 분리:

```java
// 추상화 (Interface)
public interface CalendarService {
    List<ScheduleMonthlyItemDto> getMonthlySchedules(Long memberId, int year, int month);
}

// 구현체 (Implementation)
@Service
public class CalendarServiceImpl implements CalendarService {
    // 구체적인 비즈니스 로직 구현
}
```

## 🔗 도메인 간 상호작용 분석

### 1. **Cross-Domain Service Communication**

#### **Notification ← Calendar 의존성**
```java
@Service
public class CalendarServiceImpl implements CalendarService {
    private final NotificationService notificationService; // 의존성 주입

    @Transactional
    public ScheduleCreateResponseDto createSchedule(ScheduleCreateRequestDto request, Long memberId) {
        // 1. 스케줄 생성
        Schedule savedSchedule = scheduleRepository.save(schedule);

        // 2. 알림 생성 (Cross-Domain 호출)
        notificationService.createScheduleAlarmNotifications(savedSchedule, alarmSettings);

        return responseDto;
    }
}
```

#### **Notification ← Diary 의존성**
```java
@Service
public class DiaryServiceImpl implements DiaryService {
    private final NotificationService notificationService;

    @Transactional
    public DiaryCreateResponseDto createDiary(DiaryCreateRequestDto request, Long writerId) {
        // 다이어리 생성 후 태그 알림 발송
        for (DiaryTagRequestDto tagRequest : request.getTagList()) {
            DiaryTag savedTag = diaryTagRepository.save(diaryTag);
            notificationService.createDiaryTagNotification(savedTag); // Cross-Domain 호출
        }
    }
}
```

### 2. **Event-Driven Communication 패턴**

실시간 알림 시스템에서 이벤트 기반 통신:

```java
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void createDiaryTagNotification(DiaryTag diaryTag) {
        // 1. 알림 엔티티 생성 (데이터 저장)
        Notification notification = createNotificationEntity(diaryTag);

        // 2. 실시간 알림 발송 (이벤트 발행)
        sendRealTimeNotification(notification.getMemberId(), responseDto);
    }

    private void sendRealTimeNotification(Long memberId, NotificationResponseDto responseDto) {
        // WebSocket을 통한 실시간 메시지 발송
        String directDestination = "/user/" + memberId + "/queue/notifications";
        messagingTemplate.convertAndSend(directDestination, responseDto);
    }
}
```

## 🔧 Infrastructure 계층 분석

### 1. **Spring Security 통합 아키텍처**

다층 보안 아키텍처:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // 1. CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. Session 정책 (Stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 3. JWT 필터 체인 등록
            .addFilterBefore(jwtAuthenticationFilter,
                           UsernamePasswordAuthenticationFilter.class)

            // 4. OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler))
            .build();
    }
}
```

### 2. **WebSocket 아키텍처**

STOMP 프로토콜 기반 실시간 통신:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple Broker 활성화
        config.enableSimpleBroker("/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // JWT 인증 통합
        registry.addEndpoint("/api/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
```

### 3. **JPA/Hibernate 영속성 아키텍처**

엔티티 설계 패턴:

```java
// 1. 공통 엔티티 패턴
@Entity
@Table(name = "schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 3. JPA 생명주기 훅
    @PrePersist
    public void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
```

## 📊 특수 아키텍처 패턴

### 1. **Strategy Pattern - 반복 일정 처리**

```java
@Service
public class RecurrenceServiceImpl implements RecurrenceService {

    // iCal4j 라이브러리 활용한 전략 패턴
    public List<LocalDateTime> generateRecurrenceInstances(
            String rrule, LocalDateTime startDateTime,
            LocalDateTime rangeStart, LocalDateTime rangeEnd) {

        // RFC 5545 표준 기반 반복 규칙 처리
        RecurrenceRule recurrenceRule = new RecurrenceRule(rrule);
        // 전략에 따른 인스턴스 생성
        return calculateInstances(recurrenceRule, startDateTime, rangeStart, rangeEnd);
    }
}
```

### 2. **Template Method Pattern - 다이어리 타입별 처리**

```java
@Service
public class DiaryServiceImpl implements DiaryService {

    @Transactional
    public DiaryCreateResponseDto createDiary(DiaryCreateRequestDto request, Long writerId) {
        // 1. 공통 처리 (Template)
        Diary diary = createBaseDiary(request, writerId);

        // 2. 타입별 처리 (Concrete Implementation)
        switch (request.getDiaryType()) {
            case DAILY -> processDailyDiary(diary, request.getDailyContent());
            case BOOK -> processBookDiary(diary, request.getBookContent());
            case MOVIE -> processMovieDiary(diary, request.getMovieContent());
        }

        // 3. 공통 후처리
        processTagNotifications(savedDiary, request.getTagList());
        return buildResponse(savedDiary);
    }
}
```

### 3. **Observer Pattern - 알림 시스템**

```java
// Subject: 알림 발생 주체
@Service
public class DiaryServiceImpl {
    private final NotificationService notificationService; // Observer

    private void processTagNotifications(Diary diary, List<DiaryTagRequestDto> tagList) {
        for (DiaryTagRequestDto tagRequest : tagList) {
            DiaryTag savedTag = diaryTagRepository.save(diaryTag);
            // Observer에게 이벤트 통지
            notificationService.createDiaryTagNotification(savedTag);
        }
    }
}

// Observer: 알림 처리 주체
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void createDiaryTagNotification(DiaryTag diaryTag) {
        // 이벤트 처리 및 관련 Observer들에게 전파
        Notification notification = createNotificationEntity(diaryTag);
        notificationRepository.save(notification);

        // 실시간 알림 Observer
        sendRealTimeNotification(notification.getMemberId(), responseDto);
    }
}
```

## 🔀 데이터 흐름 분석

### 1. **일정 생성 플로우**

```
Client Request → CalendarController → CalendarService →
ScheduleRepository → NotificationService → WebSocket Broadcasting
```

### 2. **실시간 알림 플로우**

```
Business Event → NotificationService → Database Storage +
WebSocket Message → STOMP Broker → Connected Clients
```

### 3. **인증/인가 플로우**

```
HTTP Request → JwtAuthenticationFilter → JwtTokenProvider →
SecurityContext → Controller → Service
```

## ⚡ 성능 및 확장성 고려사항

### 1. **트랜잭션 관리**

```java
// Read-Only 최적화
@Transactional(readOnly = true) // 기본값
public class CalendarServiceImpl {

    @Transactional // 쓰기 작업시에만 Override
    public ScheduleCreateResponseDto createSchedule(...) {
        // 트랜잭션 범위 최적화
    }
}
```

### 2. **지연 로딩 최적화**

```java
// N+1 문제 방지
@Query("SELECT s FROM Schedule s JOIN FETCH s.category WHERE s.member.id = :memberId")
List<Schedule> findSchedulesWithCategory(@Param("memberId") Long memberId);
```

### 3. **WebSocket 세션 관리**

```java
@Service
public class WebSocketSessionManager {
    private final Map<String, Long> sessionToMemberMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> memberToSessionsMap = new ConcurrentHashMap<>();

    // 동시성 안전한 세션 관리
}
```

## 🏗 아키텍처의 장단점

### ✅ **장점**

1. **모듈화**: 도메인별 명확한 분리로 유지보수성 향상
2. **확장성**: 새로운 도메인 추가 시 기존 코드 영향 최소화
3. **테스트 용이성**: 계층별 독립적 테스트 가능
4. **Spring 생태계 활용**: 검증된 패턴과 라이브러리 활용
5. **실시간 통신**: WebSocket 기반 즉각적인 사용자 경험

### ⚠️ **단점**

1. **복잡성**: 레이어 간 데이터 변환 오버헤드
2. **성능**: 도메인 간 호출로 인한 추가 비용
3. **의존성**: 도메인 간 결합도가 여전히 존재
4. **학습 곡선**: 신규 개발자의 전체 구조 이해 필요

## 🎯 결론: 아키텍처 특징 요약

PlaNa 백엔드 시스템은 다음과 같은 **복합 아키텍처 패턴**을 적용한 현대적인 웹 애플리케이션입니다:

### **1. 핵심 아키텍처**
- **레이어드 아키텍처 (Layered Architecture)**: 전통적인 3-Layer를 확장한 4-Layer 구조
- **도메인 주도 설계 (DDD) 요소**: Bounded Context 기반 도메인 모듈 분리
- **MVC 패턴**: Spring MVC 프레임워크 기반 요청-응답 처리

### **2. 설계 원칙**
- **단일 책임 원칙**: 각 계층과 클래스의 명확한 역할 분담
- **의존성 역전**: Interface 기반 느슨한 결합
- **관심사 분리**: Cross-Cutting Concerns (보안, 트랜잭션, 로깅) 분리

### **3. 특화된 패턴**
- **Strategy Pattern**: 반복 일정 처리 (iCal4j 활용)
- **Template Method**: 다이어리 타입별 처리 분기
- **Observer Pattern**: 이벤트 기반 알림 시스템

### **4. 현대적 특징**
- **실시간 통신**: WebSocket + STOMP 프로토콜
- **마이크로서비스 지향**: 도메인별 독립적 개발 가능
- **클라우드 네이티브**: Docker 컨테이너화 및 확장성 고려
- **보안 통합**: JWT + OAuth2 멀티 인증 체계

이러한 아키텍처는 **전통적인 모놀리틱 구조의 안정성**과 **현대적인 분산 시스템의 확장성**을 균형있게 제공하며, 중규모 웹 애플리케이션에 적합한 **실용적이고 유연한 설계**라고 평가할 수 있습니다.

---

**분석 일자**: 2025-09-24
**분석 범위**: Calendar, Notification, Diary, Auth 도메인 및 Infrastructure 계층
**아키텍처 스타일**: Layered + DDD Elements + Event-Driven Components