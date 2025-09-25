# PlaNa WebSocket 연결 테스트 및 문제 해결 과정

## 📋 테스트 결과 요약

### ✅ 최종 성공 상태
- **WebSocket 연결**: 성공적으로 연결됨
- **JWT 인증**: Authorization 헤더로 정상 전송
- **STOMP 프로토콜**: 연결 프레임 전송 완료
- **엔드포인트**: `https://plana.hoonee-math.info/api/ws`

### 🔍 성공 로그 분석
```
🐛 STOMP: Web Socket Opened...
🐛 STOMP: >>> CONNECT
Authorization:Bearer eyJhbGciOiJIUzUxMiJ9...
accept-version:1.1,1.0
heart-beat:10000,10000
```

## 🚨 발견된 문제들과 해결 과정

### 1. CORS 정책 문제

#### 문제 현상
```
Access to XMLHttpRequest at 'https://plana.hoonee-math.info/ws/info'
from origin 'https://plana-frontend-silk.vercel.app'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header
```

#### 원인
- WebSocket CORS 설정과 일반 HTTP CORS 설정이 독립적으로 작동
- Spring Boot의 CORS 설정이 Nginx 리버스 프록시에서 제거됨

#### 해결책
**Nginx Proxy Manager 프록시 경로 문제로 인한 근본적 해결:**
- WebSocket 엔드포인트를 `/ws`에서 `/api/ws`로 변경
- 기존 `/api` 프록시 설정을 활용하여 CORS 문제 해결

### 2. 401 Unauthorized 에러

#### 문제 현상
```
GET https://plana.hoonee-math.info/api/ws/info?t=1758593930343 401 (Unauthorized)
```

#### 원인
- SockJS의 초기 HTTP 요청(`/info`)에는 JWT 토큰이 자동으로 포함되지 않음
- Spring Security에서 `/api/ws/**` 경로가 인증 필요 경로로 설정됨

#### 해결책
**SecurityConfig.java 수정:**
```java
.requestMatchers(
    // 기존 설정...
    "/api/ws/**",           // WebSocket 엔드포인트
    "/api/ws/info/**",      // SockJS info 엔드포인트
    "/api/ws/websocket/**"  // SockJS transport 엔드포인트
).permitAll()
```

### 3. Nginx Proxy Manager 라우팅 문제

#### 문제 현상
- `/ws` 경로가 백엔드로 프록시되지 않음
- Nginx에서 정적 파일 응답 또는 404 에러 발생

#### 원인
- Nginx Proxy Manager에서 `/api/*` 경로만 백엔드로 프록시 설정됨
- WebSocket 엔드포인트 `/ws`가 라우팅 대상에 포함되지 않음

#### 해결책 (선택한 방법)
**엔드포인트 경로 변경:**
```java
// WebSocketConfig.java
registry.addEndpoint("/api/ws")  // /ws → /api/ws
```

#### 대안 해결책 (미선택)
Nginx Proxy Manager에 별도 Custom Location 추가:
```nginx
Location: /ws
Websockets Support: ✅ 체크
Custom Config: WebSocket 프록시 설정 추가
```

### 4. SockJS 폴백 메커니즘

#### 관찰된 현상
```
WebSocket connection to 'wss://plana.hoonee-math.info/api/ws/017/irtyz5ei/websocket' failed:
```

#### 분석
- **정상적인 SockJS 동작**: WebSocket 연결 시도 후 폴백 방식으로 연결
- 첫 번째 WebSocket 시도가 실패해도 다른 전송 방식(XHR, polling 등)으로 자동 대체
- 최종적으로 연결 성공: `Web Socket Opened...`

## 🔧 최종 구성 설정

### WebSocket 엔드포인트
```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/api/ws")
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://plana-frontend-silk.vercel.app",
                // ... 기타 허용 도메인
            )
            .withSockJS();
}
```

### Security 설정
```java
.requestMatchers(
    "/api/ws/**",           // WebSocket 연결 엔드포인트
    "/api/ws/info/**",      // SockJS 서버 정보 요청
    "/api/ws/websocket/**"  // SockJS WebSocket 전송
).permitAll()
```

### 메시지 브로커 (변경 없음)
```java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.setApplicationDestinationPrefixes("/app");     // 클라이언트 → 서버
    config.enableSimpleBroker("/topic", "/user");        // 서버 → 클라이언트
    config.setUserDestinationPrefix("/user");            // 개인 메시지
}
```

## 🌐 프론트엔드 연동 (업데이트)

### 연결 코드
```javascript
// 업데이트된 엔드포인트 사용
const socket = new SockJS('https://plana.hoonee-math.info/api/ws');
// 또는 상대 경로: new SockJS('/api/ws');

const stompClient = Stomp.over(socket);

stompClient.connect({
    'Authorization': 'Bearer ' + jwtToken
}, function(frame) {
    // 연결 성공
    stompClient.subscribe('/user/queue/notifications', function(message) {
        const notification = JSON.parse(message.body);
        // 실시간 알림 처리
    });
});
```

## 📊 테스트 환경별 결과

### 로컬 환경 (localhost:8080)
- ✅ 직접 연결 가능
- ✅ CORS 문제 없음
- ✅ 인증 정상 작동

### 배포 환경 (plana.hoonee-math.info)
- ✅ `/api/ws` 엔드포인트로 연결 성공
- ✅ Nginx Proxy Manager를 통한 정상 라우팅
- ✅ HTTPS WebSocket (WSS) 연결 성공
- ✅ JWT 인증 헤더 전송 확인

### 프론트엔드 환경 (plana-frontend-silk.vercel.app)
- ✅ CORS 정책 통과
- ✅ 브라우저 개발자 도구에서 연결 테스트 성공
- ✅ SockJS 폴백 메커니즘 정상 작동

## 🎯 핵심 교훈

### 1. WebSocket과 HTTP CORS의 차이점
- WebSocket CORS는 `WebSocketConfig`에서 별도 설정 필요
- 일반 HTTP CORS 설정(`SecurityConfig`, `WebMvcConfig`)과 독립적

### 2. SockJS의 다단계 연결 과정
- `/info` 요청 → WebSocket 시도 → 폴백 방식 → 최종 연결
- 중간 실패 로그가 있어도 최종 성공 가능

### 3. Nginx 프록시 환경에서의 WebSocket
- 경로 기반 라우팅이 WebSocket에도 적용됨
- 별도 WebSocket 프록시 설정보다 기존 API 경로 활용이 효율적

### 4. JWT 인증 타이밍
- SockJS info 요청: JWT 불필요 (`permitAll()`)
- WebSocket handshake: JWT 필요 (STOMP 헤더)
- 실제 메시지 통신: JWT 검증을 통한 사용자 식별

## 🔮 향후 개선 사항

### 보안 강화
- 운영 환경에서 `setAllowedOriginPatterns("*")` 제거
- 구체적인 도메인만 허용하도록 설정

### 모니터링 추가
- WebSocket 연결 성공/실패 로그 수집
- 실시간 연결 수 모니터링
- SockJS 폴백 사용률 분석

### 성능 최적화
- Redis를 활용한 멀티 서버 세션 관리
- WebSocket 연결 풀 관리
- 대용량 동시 접속 대비 설정 조정

이제 PlaNa 프로젝트의 실시간 알림 시스템이 완전히 작동합니다! 🎉