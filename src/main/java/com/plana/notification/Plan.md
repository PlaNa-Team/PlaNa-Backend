# PlaNa Backend - NotificationController 알림 기능 구현 완료 보고서

## 📋 1. 최종 구현 결과 요약

### ✅ 완료된 기능
- **통합 알림 시스템**: 다이어리 태그 + 스케줄 알림 통합 관리
- **WebSocket 실시간 알림**: JWT 인증 기반 개인별 실시간 메시징
- **REST API**: 알림 CRUD 및 상태 관리
- **스케줄러**: 예정된 알림 자동 발송
- **세션 관리**: 온라인 사용자 추적 및 최적화된 알림 발송

### 🏗️ 아키텍처 개요
```
[프론트엔드]
    ↕ WebSocket (/ws) + JWT
    ↕ REST API (/api/notifications)
[NotificationController]
    ↓
[NotificationService] ← [WebSocketSessionManager]
    ↓                    ↓
[NotificationRepository] [메모리 세션 관리]
    ↓
[Notification 엔티티] (isSent + isRead 분리)
```

## 🔍 2. 엔티티 설계 (최종)

### 2.1 Notification 엔티티 (핵심 변경사항)
```java
@Entity
public class Notification {
    // 기존 필드들...
    private Boolean isRead;     // 사용자가 실제 읽었는지
    private Boolean isSent;     // 서버에서 발송했는지 (중복 방지용)
    private LocalDateTime readAt;   // 읽은 시간
    private LocalDateTime sentAt;   // 발송 시간
}
```

**핵심 설계 원칙**: 발송 ≠ 읽음
- `isSent`: 서버 발송 완료 여부 (스케줄러 중복 방지용)
- `isRead`: 사용자 실제 확인 여부 (UI 읽음 표시용)

### 2.2 관련 엔티티 (변경사항 없음)
- **Schedule + ScheduleAlarm**: 상대적 알림 시간 저장
- **Diary + DiaryTag**: 태그 상태 관리 (PENDING → ACCEPTED/REJECTED)

## 🚀 3. Phase별 구현 내용

### Phase 1: WebSocket 기반 실시간 알림 시스템 구축 ✅

#### 구현된 파일
1. **WebSocketConfig.java**: STOMP 기반 메시징 설정
2. **NotificationController.java**: REST API + WebSocket 메시지 핸들러
3. **NotificationService.java + Impl**: 알림 비즈니스 로직
4. **NotificationRepository.java**: JPA 쿼리 메서드
5. **Response/Request DTO들**: API 응답 형식

#### 핵심 기능
- **WebSocket 엔드포인트**: `/ws` (SockJS 폴백 지원)
- **메시지 채널**: `/user/{memberId}/notifications` (개인별)
- **REST API**:
  - `GET /api/notifications`: 알림 목록 (페이징)
  - `PUT /api/notifications/{id}/read`: 읽음 처리
  - `PUT /api/notifications/read-all`: 전체 읽음

### Phase 2: 다이어리 태그 알림 통합 ✅

#### 수정된 파일
- **DiaryServiceImpl.java**: 태그 생성/상태 변경 시 알림 로직 추가

#### 구현된 로직
1. **태그 생성 시** (DiaryServiceImpl:127-137)
   ```java
   // 작성자가 아닌 다른 사용자를 태그한 경우 즉시 알림
   if (!writer.getId().equals(taggedMember.getId()) && status == TagStatus.PENDING) {
       notificationService.createDiaryTagNotification(tag.getId(), taggedMember.getId(), message);
   }
   ```

2. **태그 수락/거절 시** (DiaryServiceImpl:537-555)
   ```java
   // 작성자에게 수락/거절 결과 알림
   notificationService.createDiaryTagNotification(tag.getId(), writer.getId(), message);
   ```

### Phase 3: 스케줄 알림 통합 ✅

#### 수정된 파일
- **CalendarServiceImpl.java**: 스케줄 생성 시 알림 예약 로직 추가
- **NotificationScheduler.java**: 1분마다 예정된 알림 발송

#### 구현된 로직
1. **스케줄 생성 시** (CalendarServiceImpl:132-143)
   ```java
   // 각 ScheduleAlarm마다 Notification 생성 (isSent=false)
   notificationService.createScheduleNotification(savedAlarm.getId(), memberId, message);
   ```

2. **스케줄러** (NotificationScheduler.java)
   ```java
   @Scheduled(fixedRate = 60000) // 1분마다 실행
   public void processScheduledNotifications() {
       // isSent = false이고 time <= now인 알림들 찾아서 발송
   }
   ```

### Phase 4: WebSocket 클라이언트 연결 관리 ✅

#### 새로 구현된 파일
1. **WebSocketSessionManager.java**: 메모리 기반 세션 관리
2. **WebSocketAuthUtil.java**: JWT 토큰 추출 및 인증
3. **WebSocketEventListener.java**: 연결/해제 이벤트 처리

#### 핵심 기능
1. **세션 관리**
   ```java
   // 멀티 디바이스/탭 지원
   Map<Long, Set<String>> memberSessions; // memberId -> sessionIds
   Map<String, Long> sessionMembers;      // sessionId -> memberId
   ```

2. **자동 이벤트 처리**
   - 연결 시: JWT 인증 → 세션 등록
   - 해제 시: 자동 세션 정리 (브라우저 닫기 감지)

3. **최적화된 알림 발송**
   ```java
   if (sessionManager.isUserOnline(memberId)) {
       // 온라인: WebSocket 실시간 발송
   } else {
       // 오프라인: DB에만 저장 (다음 로그인 시 REST API로 조회)
   }
   ```

## 🌐 4. 프론트엔드 연동 가이드

### 4.1 WebSocket 연결
```javascript
// SockJS + STOMP 클라이언트 설정
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// JWT 토큰을 헤더에 포함하여 연결
stompClient.connect({
    'Authorization': 'Bearer ' + jwtToken
}, function(frame) {
    console.log('WebSocket 연결 성공:', frame);

    // 개인 알림 채널 구독
    stompClient.subscribe('/user/' + memberId + '/notifications', function(message) {
        const notification = JSON.parse(message.body);
        displayNotification(notification); // 실시간 알림 표시
    });
});
```

### 4.2 페이지 로드 시 기존 알림 조회
```javascript
// 안읽은 알림 조회
fetch('/api/notifications?unreadOnly=true', {
    headers: { 'Authorization': 'Bearer ' + jwtToken }
})
.then(response => response.json())
.then(data => {
    displayNotificationList(data.body.data);
    updateUnreadCount(data.body.pagination.unreadCount);
});
```

### 4.3 알림 읽음 처리
```javascript
// 개별 읽음
fetch('/api/notifications/' + notificationId + '/read', {
    method: 'PUT',
    headers: { 'Authorization': 'Bearer ' + jwtToken }
});

// 전체 읽음
fetch('/api/notifications/read-all', {
    method: 'PUT',
    headers: { 'Authorization': 'Bearer ' + jwtToken }
});
```

### 4.4 연결 해제 처리
```javascript
// 로그아웃 시 WebSocket 명시적 해제
function logout() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
    // JWT 토큰 제거 및 페이지 이동
}

// 브라우저 종료 시 자동 해제 (추가 작업 불필요)
window.addEventListener('beforeunload', function() {
    // WebSocket은 자동으로 끊어짐
});
```

## 📡 5. API 설계 (최종)

### 5.1 REST API (변경사항 없음)
| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| GET | `/api/notifications` | 알림 목록 조회 (페이징, 필터링) |
| PUT | `/api/notifications/{id}/read` | 개별 알림 읽음 처리 |
| PUT | `/api/notifications/read-all` | 전체 알림 읽음 처리 |
| GET | `/api/notifications/online-stats` | 온라인 사용자 통계 (관리자용) |

### 5.2 WebSocket API
| 타입 | 경로 | 설명 |
|------|------|------|
| CONNECT | `/ws` | WebSocket 연결 (JWT 인증 필요) |
| SUBSCRIBE | `/user/{memberId}/notifications` | 개인 알림 채널 구독 |
| SEND | `/app/connect` | 연결 확인 메시지 |

### 5.3 메시지 형식 (최종)
```json
{
  "id": 1,
  "type": "TAG",
  "message": "광훈님이 다이어리에 회원님을 태그했습니다",
  "time": "2025-06-28T12:30:00",
  "isRead": false,
  "readAt": null,
  "createdAt": "2025-06-28T12:30:00",
  "relatedData": {
    "diaryId": 23,
    "diaryDate": "2025-06-28",
    "writerName": "광훈",
    "diaryType": "DAILY"
  }
}
```

## 🔧 6. 기술적 상세사항

### 6.1 온라인 사용자 구분 방식
- **메모리 기반**: `ConcurrentHashMap`으로 활성 WebSocket 세션 추적
- **실시간 확인**: DB 저장 없이 메모리에서 즉시 확인
- **자동 정리**: 브라우저 종료/탭 닫기 시 `SessionDisconnectEvent`로 자동 세션 정리

### 6.2 JWT 인증 처리
- **WebSocket 연결 시**: Authorization 헤더에서 JWT 추출 및 검증
- **연결 실패**: 잘못된 토큰은 세션 관리자에 등록하지 않음
- **다중 방식 지원**: 헤더, 쿼리 파라미터 등 유연한 토큰 전달

### 6.3 알림 발송 최적화
- **온라인**: WebSocket으로 즉시 발송
- **오프라인**: DB에만 저장 (네트워크 트래픽 절약)
- **재연결**: 프론트엔드 재연결 시 REST API로 누락된 알림 조회

## ⚠️ 7. 미구현/보완 필요 사항

### 7.1 보안 강화
- **WebSocket CORS**: 운영 환경에서 Origin 제한 필요
- **Rate Limiting**: 대량 알림 발송 시 제한 로직
- **토큰 갱신**: WebSocket 연결 중 JWT 만료 시 재인증

### 7.2 성능 최적화
- **Redis 연동**: 멀티 서버 환경에서 세션 공유
- **메시지 큐**: RabbitMQ/Kafka로 대용량 알림 처리
- **배치 최적화**: 대량 알림 발송 시 배치 단위 처리

### 7.3 모니터링 및 관리
- **세션 통계**: 실시간 연결 수, 사용자 분포 등
- **알림 통계**: 발송 성공률, 읽음률 등
- **장애 복구**: WebSocket 연결 실패 시 폴백 메커니즘

### 7.4 추가 기능 확장
- **푸시 알림**: FCM 연동으로 모바일 알림
- **알림 설정**: 사용자별 알림 on/off 설정
- **알림 타입 확장**: 댓글, 좋아요 등 추가 알림 타입

## 🎯 8. 핵심 성과

### 8.1 설계 우수성
- **발송 ≠ 읽음**: 정확한 알림 상태 관리
- **통합 시스템**: 다양한 알림을 하나의 시스템으로 관리
- **확장성**: 새로운 알림 타입 추가 용이

### 8.2 성능 효율성
- **선택적 발송**: 온라인 사용자에게만 실시간 발송
- **메모리 관리**: 휘발성 세션 정보는 메모리에서만 관리
- **네트워크 최적화**: 불필요한 WebSocket 메시지 방지

### 8.3 사용자 경험
- **즉시성**: 온라인 사용자는 실시간 알림 수신
- **일관성**: 오프라인 사용자도 로그인 시 누락 없이 알림 확인
- **안정성**: 브라우저 종료/네트워크 끊김 시 자동 복구

이제 프론트엔드에서 위 가이드를 따라 구현하면 완전한 실시간 알림 시스템을 사용할 수 있습니다!