# PlaNa Backend - NotificationController 알림 기능 분석 및 구현 계획

## 1. 사전 분석 결과

### 1.1 엔티티 분석 결과

#### Notification 엔티티 (통합 알림 설계)
```java
// 위치: com.plana.notification.entity.Notification
- scheduleAlarm: ScheduleAlarm 참조 (일정 알림)
- diaryTag: DiaryTag 참조 (다이어리 태그 알림)
- member: 알림 받을 사용자
- type: 알림 유형 ("TAG" / "ALARM")
- time: 실제 알림 발생 시각
- isRead: 읽음 여부
```

#### Schedule 관련 엔티티
```java
// Schedule: 일정 메인 엔티티
- startAt: 일정 시작 시간
- recurrenceRule: 반복 규칙 (RFC 5545 RRule)
- recurrenceUntil: 반복 종료일

// ScheduleAlarm: 알림 설정
- notifyBeforeVal: 알림 시간 숫자 (예: 5, 30)
- notifyUnit: 시간 단위 (MIN, HOUR, DAY)
```

#### Diary 관련 엔티티
```java
// DiaryTag: 다이어리 태그
- diary: 연결된 다이어리
- member: 태그된 사용자 (nullable - 비회원 태그 가능)
- tagStatus: 태그 상태 (WRITER, PENDING, ACCEPTED, REJECTED, DELETED)
- tagText: 직접 입력 태그 텍스트
```

### 1.2 기존 비즈니스 로직 분석

#### Diary 태그 처리 로직 (DiaryServiceImpl:101-156)
- 자동 상태 결정: 작성자 본인 = WRITER, 다른 사용자 = PENDING
- 두 가지 태그 타입: 회원 태그 vs 텍스트 태그
- 중복 방지: 같은 날짜에 하나의 다이어리만 수락 가능
- 태그 상태 변경: 태그된 본인만 ACCEPTED/REJECTED 변경 가능

#### Schedule 알림 처리 로직 (CalendarServiceImpl:120-130)
- 다중 알림: 하나의 일정에 여러 알림 설정 가능
- 상대적 시간: "5분전", "1시간전" 등으로 저장
- 반복 일정: RecurrenceService로 인스턴스 동적 생성

### 1.3 현재 구현 상태

#### 완전 구현된 기능
1. 다이어리 태그 시스템: 생성, 수락/거절, 상태 관리
2. 스케줄 알림 설정: ScheduleAlarm 엔티티 생성/저장
3. 반복 일정 처리: RRule 기반 인스턴스 생성
4. 통합 알림 엔티티: Notification 설계 완료

#### 미구현된 기능
1. 실제 알림 발송 시스템: Notification 데이터 생성 로직 없음
2. WebSocket 통신: 의존성 및 설정 완전 없음
3. 알림 스케줄러: 배치 작업 없음
4. NotificationController: 컨트롤러 완전 미구현

## 2. 구현 계획

### 2.1 통합 알림 시스템 설계 분석

#### 통합 알림의 핵심 요구사항
1. 다이어리 태그 알림
   - 친구가 다이어리에 태그 시 즉시 알림
   - 알림 내용: 작성자 정보, 다이어리 정보
   - 수락/거절 가능한 알림

2. 스케줄 알림
   - 설정된 시간(notifyBeforeVal + notifyUnit)에 따라 알림
   - 반복 일정 고려 필요
   - 알림 내용: 스케줄 정보

#### WebSocket vs HTTP 폴링 검토
WebSocket 장점:
- 실시간 양방향 통신
- 서버 푸시 가능
- 연결 유지로 즉시 알림

HTTP 폴링 단점:
- 지속적인 요청으로 서버 부하
- 실시간성 떨어짐
- 배터리 소모

결론: WebSocket 방식 채택

### 2.2 구현 단계별 계획

#### Phase 1: WebSocket 기반 실시간 알림 시스템 구축

1.1 의존성 추가 (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

1.2 WebSocket 설정
```java
// WebSocketConfig: STOMP 기반 메시징 설정
- /app 접두사: 클라이언트 → 서버 메시지
- /topic 접두사: 서버 → 클라이언트 브로드캐스트
- /user 접두사: 개인별 알림
```

1.3 NotificationController 구현
```java
// 기능:
- GET /api/notifications: 알림 목록 조회 (페이징)
- PUT /api/notifications/{id}/read: 알림 읽음 처리
- WebSocket /app/connect: 클라이언트 연결
```

1.4 NotificationService 구현
```java
// 기능:
- createDiaryTagNotification(): 다이어리 태그 알림 생성
- createScheduleNotification(): 스케줄 알림 생성
- sendRealTimeNotification(): WebSocket으로 실시간 발송
- markAsRead(): 알림 읽음 처리
```

#### Phase 2: 다이어리 태그 알림 통합

2.1 DiaryServiceImpl 수정
- createDiary() 메서드에서 친구 태그 시 알림 생성
- updateDiaryTagStatus() 메서드에서 수락/거절 시 알림 발송

2.2 다이어리 태그 알림 로직
```java
// DiaryTag 생성 시 (tagStatus = PENDING인 경우)
if (tagDto.getMemberId() != null && !writer.getId().equals(taggedMember.getId())) {
    // Notification 생성
    Notification notification = Notification.builder()
        .diaryTag(savedTag)
        .member(taggedMember)
        .type("TAG")
        .time(LocalDateTime.now())
        .isRead(false)
        .build();

    // 실시간 알림 발송
    notificationService.sendRealTimeNotification(notification);
}
```

#### Phase 3: 스케줄 알림 통합

3.1 스케줄 알림 생성 시점
- CalendarServiceImpl.createSchedule(): 일정 생성 시
- CalendarServiceImpl.updateSchedule(): 일정 수정 시

3.2 알림 시각 계산 로직
```java
// ScheduleAlarm → Notification 변환
for (ScheduleAlarm alarm : schedule.getAlarms()) {
    LocalDateTime notifyTime = calculateNotifyTime(
        schedule.getStartAt(),
        alarm.getNotifyBeforeVal(),
        alarm.getNotifyUnit()
    );

    // 반복 일정 고려
    if (schedule.getIsRecurring()) {
        generateRecurringNotifications(schedule, alarm, notifyTime);
    }
}
```

3.3 알림 스케줄러 구현
```java
@Scheduled(fixedRate = 60000) // 1분마다 실행
public void processScheduledNotifications() {
    LocalDateTime now = LocalDateTime.now();
    List<Notification> dueNotifications =
        notificationRepository.findDueNotifications(now);

    for (Notification notification : dueNotifications) {
        sendRealTimeNotification(notification);
    }
}
```

#### Phase 4: WebSocket 메시지 처리

4.1 클라이언트 연결 관리
```java
@MessageMapping("/connect")
public void handleConnect(SimpMessageHeaderAccessor headerAccessor) {
    String memberId = extractMemberIdFromToken(headerAccessor);
    // 사용자별 세션 관리
}
```

4.2 실시간 알림 발송
```java
public void sendRealTimeNotification(Notification notification) {
    String destination = "/user/" + notification.getMember().getId() + "/notifications";
    messagingTemplate.convertAndSend(destination, convertToDto(notification));
}
```

### 2.3 API 설계

#### 2.3.1 REST API
```
GET /api/notifications?page=1&size=20&unreadOnly=true
- 알림 목록 조회 (페이징)
- unreadOnly: 안읽은 알림만 조회

PUT /api/notifications/{id}/read
- 특정 알림 읽음 처리

PUT /api/notifications/read-all
- 모든 알림 읽음 처리
```

#### 2.3.2 WebSocket API
```
CONNECT /ws
- WebSocket 연결

SUBSCRIBE /user/{memberId}/notifications
- 개인 알림 구독

SEND /app/connect
- 클라이언트 연결 알림
```

### 2.4 예상 메시지 포맷

#### 다이어리 태그 알림
```json
{
  "id": 1,
  "type": "TAG",
  "message": "광훈님이 다이어리에 회원님을 태그했습니다",
  "time": "2025-06-28T12:30:00",
  "isRead": false,
  "relatedData": {
    "diaryId": 23,
    "diaryDate": "2025-06-28",
    "writerName": "광훈",
    "diaryType": "DAILY"
  }
}
```

#### 스케줄 알림
```json
{
  "id": 2,
  "type": "ALARM",
  "message": "10분 후 '프로젝트 회의'가 시작됩니다",
  "time": "2025-06-28T14:50:00",
  "isRead": false,
  "relatedData": {
    "scheduleId": 123,
    "scheduleTitle": "프로젝트 회의",
    "startAt": "2025-06-28T15:00:00"
  }
}
```

## 3. 기술적 고려사항

### 3.1 성능 최적화
- WebSocket 연결 관리: 사용자별 세션 맵 관리
- 배치 처리: 대량 알림 발송 시 배치 단위 처리
- 캐싱: Redis를 활용한 온라인 사용자 캐시

### 3.2 확장성 고려
- 메시지 브로커: 향후 RabbitMQ/Kafka 확장 가능
- 마이크로서비스: 알림 서비스 분리 가능
- Push 알림: FCM 연동으로 모바일 알림 확장

### 3.3 예외 처리
- WebSocket 연결 실패: HTTP 폴백 메커니즘
- 알림 발송 실패: 재시도 로직 및 Dead Letter Queue
- 대량 알림: Rate Limiting 적용

## 4. 구현 순서

### 우선순위 1 (핵심 기능)
1. WebSocket 설정 및 NotificationController 기본 구현
2. NotificationService 기본 CRUD 구현
3. 다이어리 태그 알림 연동
4. 기본 실시간 알림 발송

### 우선순위 2 (스케줄 알림)
1. 스케줄 알림 생성 로직
2. 알림 스케줄러 구현
3. 반복 일정 알림 처리

### 우선순위 3 (최적화)
1. 성능 테스트 및 최적화
2. 예외 처리 강화
3. 모니터링 및 로깅 추가

## 5. 테스트 계획

### 5.1 단위 테스트
- NotificationService 메서드별 테스트
- 알림 생성 로직 테스트
- WebSocket 메시지 발송 테스트

### 5.2 통합 테스트
- 다이어리 태그 → 알림 발송 플로우
- 스케줄 알림 → 실시간 발송 플로우
- WebSocket 연결 및 메시지 수신 테스트

### 5.3 부하 테스트
- 동시 WebSocket 연결 테스트
- 대량 알림 발송 성능 테스트

이 계획에 따라 단계별로 구현하면 요구사항에 맞는 통합 알림 시스템을 완성할 수 있습니다.