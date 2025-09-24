# PlaNa WebSocket 실시간 알림 시스템 - 최종 구현 완료 보고서

## 🎉 최종 성과 요약

### ✅ 성공적으로 해결된 문제들
1. **WebSocket 연결 실패** → **순수 WebSocket + JWT 인증 성공**
2. **메시지 라우팅 문제** → **직접 경로 방식으로 해결**
3. **Spring Security 설정 충돌** → **WebSocket 전용 허용 경로 설정**
4. **SockJS 폴백 문제** → **순수 WebSocket과 SockJS 분리 엔드포인트**
5. **중복 메시지 발송** → **단일 발송 방식으로 최적화**

### 🚀 현재 작동 상태
- ✅ **실시간 메시지 수신**: 브라우저에서 즉시 알림 수신 가능
- ✅ **JWT 인증**: 핸드셰이크 시 토큰 검증 성공
- ✅ **자동 테스트**: 10초마다 자동 테스트 메시지 발송
- ✅ **수동 테스트**: 개발자 도구에서 언제든 테스트 가능
- ✅ **실제 알림**: Postman으로 다이어리 태그 시 실시간 알림

---

## 📁 파일 구조 및 역할 분석

### 🔧 핵심 백엔드 파일들

#### 1. **WebSocket 설정 파일들**

**`JwtHandshakeInterceptor.java`**
- **용도**: WebSocket 연결 시 JWT 토큰 인증
- **호출 위치**: WebSocket 핸드셰이크 시 자동 호출
- **핵심 기능**:
  - Authorization 헤더 또는 쿼리 파라미터에서 JWT 추출
  - JwtTokenProvider를 통해 토큰 검증
  - 인증 성공 시 사용자 정보를 세션 속성에 저장
  - 인증 실패 시 WebSocket 연결 거부

**`WebSocketConfig.java`**
- **용도**: STOMP 엔드포인트 및 메시지 브로커 설정
- **핵심 변경사항**: 순수 WebSocket과 SockJS 분리
  ```java
  // 순수 WebSocket (현재 사용)
  registry.addEndpoint("/api/ws")
      .addInterceptors(jwtHandshakeInterceptor);

  // SockJS 폴백 (필요시 사용)
  registry.addEndpoint("/api/ws-sockjs")
      .addInterceptors(jwtHandshakeInterceptor)
      .withSockJS();
  ```

#### 2. **메시지 발송 및 세션 관리 파일들**

**`NotificationServiceImpl.java`**
- **용도**: 알림 생성, 저장, 실시간 발송 통합 관리
- **호출 위치**:
  - DiaryServiceImpl (다이어리 태그 생성 시)
  - CalendarServiceImpl (스케줄 알람 생성 시)
  - NotificationScheduler (예정된 알림 발송 시)
- **핵심 메서드**: `sendRealTimeNotification()`
  ```java
  // 최종 해결된 발송 방식
  String directDestination = "/user/" + memberId + "/queue/notifications";
  messagingTemplate.convertAndSend(directDestination, responseDto);
  ```

**`WebSocketSessionManager.java`**
- **용도**: 온라인 사용자 세션 추적 및 관리
- **저장 방식**: 메모리 기반 ConcurrentHashMap
- **핵심 기능**:
  - 사용자별 다중 세션 지원 (여러 탭/디바이스)
  - 세션 자동 정리 (브라우저 종료 감지)
  - 온라인 상태 실시간 확인

**`WebSocketEventListener.java`**
- **용도**: WebSocket 연결/해제 이벤트 자동 처리
- **호출 시점**: Spring이 자동으로 이벤트 발생 시 호출
- **핵심 기능**:
  - 연결 시: 핸드셰이크 인터셉터의 세션 속성에서 사용자 정보 추출
  - 해제 시: 세션 매니저에서 해당 세션 제거

#### 3. **테스트 및 디버깅 파일들**

**`WebSocketTestService.java`** 🧪
- **용도**: 개발/디버깅용 자동 테스트 메시지 발송
- **실행 방식**: `@Scheduled(fixedDelay = 10000)` - 10초마다 자동 실행
- **테스트 내용**: 온라인 사용자들에게 현재 시간이 포함된 테스트 메시지 발송
- **로그 확인**: 백엔드 콘솔에서 `테스트 메시지 발송: memberId=24, time=18:24:28` 형태로 확인

**`NotificationController.java`** - 테스트 API 추가
- **수동 테스트 엔드포인트**: `POST /api/notifications/test-message`
- **호출 방법**:
  ```javascript
  fetch('http://localhost:8080/api/notifications/test-message', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('accessToken') }
  })
  ```

#### 4. **보안 설정 파일**

**`SecurityConfig.java`** - WebSocket 전용 설정 추가
- **핵심 변경사항**: WebSocket 관련 모든 경로 허용
  ```java
  .requestMatchers(
      "/api/ws/**",              // WebSocket 엔드포인트 (모든 하위 경로)
      "/api/ws/info/**",         // SockJS info 엔드포인트
      "/api/ws/websocket/**",    // SockJS transport 엔드포인트
      "/api/ws/*/websocket/**",  // SockJS 세션별 WebSocket
      "/api/ws/*/xhr/**",        // SockJS XHR 폴백
      "/api/ws/*/jsonp/**",      // SockJS JSONP 폴백
      "/api/ws/*/iframe.html"    // SockJS iframe
  ).permitAll()
  ```
- **X-Frame-Options 설정**: SockJS iframe 지원을 위해 SAMEORIGIN 설정

---

## 🔄 메시지 흐름 상세 분석

### 📡 구독(Subscribe) 과정

#### 1. **프론트엔드에서 구독 시작**
```javascript
// 브라우저에서 실행
window.stompClient.subscribe('/user/queue/notifications', function(message) {
    console.log('🔔 알림 수신:', JSON.parse(message.body));
});
```

#### 2. **Spring STOMP가 구독 처리**
- **내부 변환**: `/user/queue/notifications` → `/user/24/queue/notifications`
- **세션 매핑**: JWT에서 추출한 사용자 ID와 WebSocket 세션 연결
- **구독 등록**: 해당 경로로 오는 메시지를 이 세션에 전달하도록 설정

#### 3. **백엔드에서 구독 확인**
```java
// WebSocketEventListener.java에서 자동 로깅
@EventListener
public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
    log.info("개인 알림 채널 구독 성공: memberId={}, destination={}", memberId, destination);
}
```

### 📤 메시지 발송(Publish) 과정

#### 1. **알림 이벤트 발생**
- **다이어리 태그**: `DiaryServiceImpl.createDiaryTag()` → `notificationService.createDiaryTagNotification()`
- **스케줄 알람**: `NotificationScheduler.processScheduledNotifications()` → `notificationService.sendRealTimeNotification()`
- **수동 테스트**: `NotificationController.sendTestMessage()` → 직접 메시지 발송

#### 2. **온라인 상태 확인**
```java
// NotificationServiceImpl.java
boolean isOnline = sessionManager.isUserOnline(memberId);
if (isOnline) {
    // 실시간 발송
} else {
    // DB에만 저장
}
```

#### 3. **메시지 발송 (현재 해결된 방식)**
```java
// 직접 경로로 발송 (Spring 자동 라우팅 대신)
String directDestination = "/user/" + memberId + "/queue/notifications";
messagingTemplate.convertAndSend(directDestination, responseDto);
```

#### 4. **브라우저에서 수신**
```javascript
// 구독 핸들러에서 자동 수신
function(message) {
    console.log('🔔 알림 수신:', JSON.parse(message.body));
}
```

### 🔄 완전한 테스트 흐름 예시

**다이어리 태그 알림 전체 흐름:**

1. **Postman**: 다른 사용자가 24번 사용자를 태그한 다이어리 작성
2. **DiaryServiceImpl**: 태그 생성 감지 → `createDiaryTagNotification()` 호출
3. **NotificationServiceImpl**:
   - 알림 DB 저장 (Notification 엔티티)
   - 온라인 상태 확인 (`sessionManager.isUserOnline(24)`)
   - 실시간 발송 (`messagingTemplate.convertAndSend()`)
4. **Spring STOMP**: `/user/24/queue/notifications` 경로로 메시지 라우팅
5. **브라우저**: 구독 핸들러에서 메시지 수신 → 콘솔 출력

---

## 🛠️ 주요 문제와 해결 과정

### ❌ 문제 1: Spring의 `convertAndSendToUser` 작동 실패

**증상**:
```java
messagingTemplate.convertAndSendToUser("24", "/queue/notifications", message);
```
→ 메시지가 브라우저에 도달하지 않음

**원인**: Spring STOMP의 사용자 매핑이 제대로 설정되지 않음

**해결책**: 직접 경로 방식 사용
```java
String directDestination = "/user/" + memberId + "/queue/notifications";
messagingTemplate.convertAndSend(directDestination, responseDto);
```

### ❌ 문제 2: SockJS iframe, JSONP 폴백 404 오류

**증상**:
```
GET http://localhost:8080/api/ws/iframe.html 404 (Not Found)
GET http://localhost:8080/api/ws/251/jsonp 404 (Not Found)
```

**원인**: Spring Security가 SockJS 보조 경로들을 차단

**해결책**:
1. **SecurityConfig**에서 모든 WebSocket 경로 허용
2. **순수 WebSocket 엔드포인트 분리** - 복잡한 SockJS 폴백 없이도 작동

### ❌ 문제 3: 중복 메시지 수신

**증상**: 동일한 알림이 2번씩 브라우저에 표시됨

**원인**: 백엔드에서 두 가지 방식으로 동시 발송
```java
// 이전 코드 (문제)
messagingTemplate.convertAndSend(directDestination, responseDto);
messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/notifications", responseDto);
```

**해결책**: 단일 발송 방식으로 통합

---

## 🎯 메시지 형식 표준화

### 📋 공통 메시지 구조
모든 알림(테스트, 다이어리 태그, 스케줄 알람)은 동일한 구조로 브라우저에 전송됩니다:

```json
{
  "id": 23,
  "type": "TAG|ALARM|TEST|MANUAL_TEST",
  "message": "사용자에게 보여질 메시지",
  "time": "2025-09-23T18:26:21.358011800",
  "isRead": false,
  "createdAt": "2025-09-23T18:26:21",
  "relatedData": {
    // 타입별 추가 데이터
  }
}
```

### 📝 타입별 메시지 예시

**테스트 메시지 (WebSocketTestService)**:
```json
{
  "type": "TEST",
  "message": "WebSocket 연결 테스트 메시지",
  "time": "18:24:28",
  "memberId": 24
}
```

**수동 테스트 메시지 (NotificationController)**:
```json
{
  "type": "MANUAL_TEST",
  "message": "수동 테스트 메시지입니다!",
  "time": "2025-09-23T18:26:21.358011800",
  "memberId": 24
}
```

**다이어리 태그 알림**:
```json
{
  "id": 23,
  "type": "TAG",
  "message": "홍길동님이 다이어리에 회원님을 태그했습니다",
  "time": "2025-09-23T18:30:00",
  "isRead": false,
  "relatedData": {
    "diaryId": 45,
    "diaryDate": "2025-09-23",
    "writerName": "홍길동",
    "diaryType": "DAILY"
  }
}
```

---

## 🔧 새로운 WebSocket 서비스 구현 가이드

### 1. **백엔드 서비스 추가**

새로운 알림 타입을 추가하려면:

```java
// 1. NotificationService에 새 메서드 추가
public interface NotificationService {
    NotificationResponseDto createCustomNotification(Long targetMemberId, String customData);
}

// 2. NotificationServiceImpl에 구현
@Override
public NotificationResponseDto createCustomNotification(Long targetMemberId, String customData) {
    // 알림 DB 저장
    Notification notification = Notification.builder()
            .member(targetMember)
            .type("CUSTOM")
            .message("커스텀 알림 메시지")
            .time(LocalDateTime.now())
            .isRead(false)
            .build();

    Notification savedNotification = notificationRepository.save(notification);

    // 실시간 발송
    sendRealTimeNotification(savedNotification);

    return convertToResponseDto(savedNotification);
}

// 3. 호출하는 서비스에서 사용
// 예: CommentService, LikeService 등
@Autowired
private NotificationService notificationService;

public void createComment(CommentRequestDto request) {
    // 댓글 저장 로직...

    // 게시글 작성자에게 알림
    notificationService.createCustomNotification(postAuthorId, "새 댓글이 달렸습니다");
}
```

### 2. **프론트엔드 구독 코드**

새로운 서비스도 **동일한 구독 경로**를 사용합니다:

```javascript
// 모든 알림은 같은 채널로 수신
stompClient.subscribe('/user/queue/notifications', function(message) {
    const data = JSON.parse(message.body);

    // 타입별 처리
    switch(data.type) {
        case 'TAG':
            showDiaryTagNotification(data);
            break;
        case 'ALARM':
            showScheduleAlarmNotification(data);
            break;
        case 'CUSTOM':
            showCustomNotification(data);
            break;
        case 'TEST':
        case 'MANUAL_TEST':
            console.log('🧪 테스트:', data.message);
            break;
    }
});
```

### 3. **테스트 방법**

```java
// Controller에 테스트 엔드포인트 추가
@PostMapping("/test-custom")
public ResponseEntity<ApiResponse<String>> testCustomNotification(
        @AuthenticationPrincipal AuthenticatedMemberDto authMember) {

    notificationService.createCustomNotification(authMember.getId(), "테스트 데이터");
    return ResponseEntity.ok(ApiResponse.success("커스텀 알림 테스트 발송"));
}
```

---

## 🔍 개발자 도구 테스트 가이드

### 🚀 브라우저 테스트 스크립트 사용법

현재 제공되는 완전한 테스트 스크립트([브라우저테스트용스크립트.js](%EB%B8%8C%EB%9D%BC%EC%9A%B0%EC%A0%80%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%9A%A9%EC%8A%A4%ED%81%AC%EB%A6%BD%ED%8A%B8.js))를 개발자 도구에서 실행:

1. **F12** → Console 탭
2. **스크립트 전체 복사 & 붙여넣기**
3. **자동으로 연결 시도** → 성공 시 `🎉 순수 WebSocket 연결 성공!` 출력

### 🧪 테스트 명령어들

연결 후 사용 가능한 명령어들:

```javascript
// 연결 상태 확인
checkConnection();

// 수동 테스트 메시지 발송
sendTestMessage();

// 재연결 (문제 발생 시)
testPureWebSocket();
```

### 📊 로그 해석 가이드

**성공적인 연결 시 나타나는 로그:**
```
🔥 순수 WebSocket 연결 테스트 (라이브러리 자동 로딩)
🔑 토큰 확인 완료: 260자
🔌 WebSocket 연결 중: ws://localhost:8080/api/ws?token=...
🟢 WebSocket 연결 열림
✅ STOMP 연결 성공!
📫 구독 완료: /user/24/queue/notifications (ID: pure-sub-1)
📤 세션 등록 완료
```

**메시지 수신 시 나타나는 로그:**
```
🔧 STOMP: <<< MESSAGE
destination:/user/24/queue/notifications
content-type:application/json
subscription:pure-sub-1
message-id:86d1df31-6f45-5de0-ddea-a5004b79f9f7-44

🎯 [/user/24/queue/notifications] 메시지 수신!
📦 Body: {"time":"18:24:28","type":"TEST","message":"WebSocket 연결 테스트 메시지","memberId":24}
🎯 [/user/24/queue/notifications] 파싱된 데이터: {time: '18:24:28', type: 'TEST', ...}
```

### 🔄 백엔드 로그 확인

백엔드 콘솔에서 확인할 수 있는 로그들:

```
# 연결 성공
WebSocket 핸드셰이크 인증 성공: memberId=24, email=test@example.com

# 세션 등록
사용자 세션 등록: memberId=24, sessionId=abc123, 총 세션 수=1

# 메시지 발송
실시간 알림 발송 완료: memberId=24, destination=/user/24/queue/notifications

# 자동 테스트 (10초마다)
테스트 메시지 발송: memberId=24, time=18:24:28
```

---

## 📚 추가 참고 파일들

### 🗂️ 관련 문서들
- **`Plan.md`**: 초기 설계 및 요구사항 정의
- **`README_WebSocket.md`**: 이전 테스트 과정 기록 (문제점 포함)
- **`브라우저테스트용스크립트.js`**: 완성된 테스트 스크립트

### 🔧 핵심 설정 파일들
- **`application.properties`**: JWT TTL 및 기본 설정
- **`WebSocketConfig.java`**: STOMP 엔드포인트 설정
- **`SecurityConfig.java`**: WebSocket 보안 설정

---

## 🎯 성공 요인 분석

### 1. **문제 해결 접근법**
- **단계적 문제 분리**: WebSocket 연결 → 인증 → 메시지 라우팅 → 구독 처리
- **디버깅 도구 활용**: 브라우저 Network 탭, 백엔드 로그, STOMP 디버그 메시지
- **대안 방식 탐색**: Spring 표준 방식 실패 시 직접 경로 방식으로 전환

### 2. **아키텍처 설계 우수성**
- **관심사 분리**: 인증(JwtHandshakeInterceptor), 세션 관리(WebSocketSessionManager), 메시지 발송(NotificationService)
- **확장성 고려**: 새로운 알림 타입 추가 용이
- **테스트 용이성**: 자동/수동 테스트 도구 완비

### 3. **개발 효율성**
- **실시간 디버깅**: 브라우저 콘솔에서 즉시 테스트 가능
- **자동화된 테스트**: 10초마다 자동 메시지로 연결 상태 확인
- **명확한 로그**: 각 단계별 상세한 로그로 문제 추적 용이

---

## 🚀 프로덕션 배포 체크리스트

### ⚠️ 보안 강화 필요사항
- [ ] WebSocket CORS 설정: `setAllowedOriginPatterns("*")` → 구체적 도메인 설정
- [ ] JWT 토큰 만료 시 WebSocket 재연결 로직
- [ ] Rate Limiting: 대량 메시지 발송 제한

### 🔧 성능 최적화 권장사항
- [ ] Redis 연동: 멀티 서버 환경 세션 공유
- [ ] Connection Pool 설정: 대용량 동시 접속 대응
- [ ] 메시지 배치 처리: 대량 알림 발송 시 성능 개선

### 📊 모니터링 추가 권장사항
- [ ] WebSocket 연결 수 실시간 모니터링
- [ ] 알림 발송 성공률 추적
- [ ] 사용자별 알림 수신 통계

---

## 🎉 최종 결론

PlaNa 프로젝트의 **실시간 WebSocket 알림 시스템이 완전히 구축되고 정상 작동**하고 있습니다.

### 🌟 주요 달성사항
1. ✅ **실시간 알림**: 다이어리 태그, 스케줄 알람 즉시 수신
2. ✅ **JWT 기반 인증**: 보안성 확보된 WebSocket 연결
3. ✅ **세션 관리**: 온라인/오프라인 사용자 구분 최적화
4. ✅ **확장성**: 새로운 알림 타입 추가 용이한 구조
5. ✅ **테스트 완비**: 개발자 도구를 통한 실시간 테스트 가능

### 💡 핵심 교훈
- **Spring 표준이 항상 최선은 아니다**: `convertAndSendToUser` 실패 → 직접 경로 성공
- **디버깅 도구의 중요성**: 상세한 로그와 테스트 스크립트로 문제 해결 가속화
- **단계적 접근**: 복잡한 SockJS → 순수 WebSocket으로 단순화하여 성공

이제 **프론트엔드에서 이 가이드를 따라 구현하면 완전한 실시간 알림 시스템을 사용할 수 있습니다!** 🚀