# Calendar 모듈 개발 계획

## 🎯 목표
Spring Boot 환경에서 RFC 5545 RRule 기반의 반복 일정 관리 시스템 구축

## 📋 단계별 실행 계획

### Phase 1: 기반 설정 및 Entity 개선
1. **RRule 처리 라이브러리 추가**
   - `ical4j` 라이브러리를 pom.xml에 추가 (RRULE 전용, 가벼움)
   - Maven dependency 설정

2. **Entity 시간 필드 개선 (보류)**
   - `LocalDateTime` → `ZonedDateTime` 변경 → **한국 전용 서비스로 현재 보류**
   - **현재 결정**: `LocalDateTime` 유지 (KST 기준)
   - **향후 고려사항**: 
     - 구글 캘린더, 아웃룩 등 외부 API 연동 시 `ZonedDateTime` 필요
     - 글로벌 서비스 확장 시 시간대 정보 보존 필수
     - Entity에 시간대 관련 주석 추가로 향후 마이그레이션 가이드 제공

3. **RRule 처리 유틸리티 클래스 생성**
   - RRule 파싱 및 검증
   - 반복 일정 인스턴스 생성
   - 날짜 범위 내 반복 일정 계산

### Phase 2: Repository 및 Service 구현
4. **Repository 인터페이스 생성**
   - ScheduleRepository, CategoryRepository 등
   - 커스텀 쿼리 메서드 정의

5. **Service 계층 구현**
   - 월별 일정 조회 (반복 일정 인스턴스 포함)
   - 일정 상세 조회
   - 일정 생성/수정/삭제 (논리적 삭제)

### Phase 3: API 구현
6. **DTO 클래스 생성** ✅
   - Request/Response DTO 구조화 완료
   - 기존 프로젝트 패턴(Diary 모듈) 준수
   - API별 사용처 및 의존관계 주석 추가

7. **Controller 구현**
   - `GET /api/calendars?year=2025&month=6` (월별 조회)
   - `GET /api/calendars/{id}` (상세 조회)
   - `POST /api/calendars` (생성)
   - `PATCH /api/calendars/{id}` (부분 수정) - REST API 관례 준수
   - `DELETE /api/calendars/{id}` (삭제)

### Phase 4: 알림 시스템 기초 구현
8. **알림 스케줄링**
   - Spring `@Scheduled` 기반 알림 체크
   - ScheduleAlarm 기반 알림 발생 시간 계산

## 🔧 주요 기술 스택
- **RRule 처리**: ical4j 라이브러리
- **시간 관리**: ZonedDateTime (UTC 기준)
- **반복 일정**: RFC 5545 RRule 형식
- **알림**: Spring Scheduler + 커스텀 구현

## 📁 파일 생성 목록
- `/calendar/Plan.md` (현재 문서)
- `/calendar/utils/RRuleUtils.java` (RRule 처리 유틸리티)
- `/calendar/dto/request/*.java` (Request DTO들)
- `/calendar/dto/response/*.java` (Response DTO들)
- `/calendar/repository/*.java` (Repository 인터페이스들)
- `/calendar/service/*.java` (Service 인터페이스 및 구현)

## 💡 추가 제안사항

### 성능 및 확장성 고려사항
1. **반복 일정 저장 전략**
   - 현재: RRule 문자열만 저장 → 조회 시 실시간 계산
   - 제안: 자주 조회되는 범위(향후 3개월)의 인스턴스 미리 생성하여 별도 테이블 저장
   - 장점: 월별 조회 성능 대폭 향상, 복잡한 RRule 처리 부담 감소

2. **캐싱 전략**
   - 월별 일정 조회 결과 Redis 캐싱 (TTL: 1시간)
   - 카테고리 정보 애플리케이션 레벨 캐싱
   - 반복 일정 계산 결과 캐싱으로 중복 계산 방지

3. **데이터 일관성 및 무결성**
   - 반복 일정의 개별 인스턴스 수정 시 처리 전략 필요
   - "이 일정만 수정" vs "이후 모든 일정 수정" 옵션 제공
   - 원본 Schedule과 예외 인스턴스 관계 테이블 추가 고려

4. **API 최적화**
   - 월별 조회 시 필요한 필드만 선택적 반환 (Projection 활용)
   - 페이징 처리 (특히 반복 일정이 많은 경우)
   - 배치 작업을 통한 알림 미리 생성

### 외부 연동 대비사항
5. **외부 캘린더 API 연동 준비**
   - 구글 캘린더, 네이버 캘린더 등과의 동기화 고려
   - External ID 필드 추가로 매핑 정보 저장
   - 시간대 변환 유틸리티 클래스 미리 준비

6. **국제화 확장성**
   - 카테고리명, 알림 메시지의 다국어 지원 테이블 구조 고려
   - 시간 표시 형식의 로케일 지원

## 📋 **Phase 2 진행 상황 - DTO 및 Service 설계 완료**

### **✅ 완료된 DTO 작업**
#### **Request DTO:**
- `ScheduleCreateRequestDto` - POST /api/calendars (일정 생성)
- `ScheduleUpdateRequestDto` - PATCH /api/calendars/{id} (부분 수정)
- `ScheduleAlarmRequestDto` - 알림 설정 (위 DTO들의 중첩 객체)

#### **Response DTO:**
- `ScheduleDetailResponseDto` - GET /api/calendars/{id} (상세 조회)
- `ScheduleCreateResponseDto` - POST /api/calendars (생성 응답)
- `ScheduleMonthlyResponseDto` - GET /api/calendars (월별 조회 컨테이너)
- `ScheduleMonthlyItemDto` - 월별 조회 개별 아이템
- `CategoryResponseDto` - 카테고리 정보 (중첩 객체)
- `ScheduleAlarmResponseDto` - 알림 정보 (중첩 객체)
- `ApiResponse<T>` - 공통 응답 래퍼

#### **DTO 설계 결정사항:**
- ❌ `memberId` 필드 제거 → JWT에서 추출하여 보안 강화
- ✅ `alarms` 복수형 사용 → 다중 알림 지원
- ✅ PATCH 방식 채택 → REST API 관례 준수 (부분 수정)
- ✅ 중첩 DTO 구조 → 가독성 및 재사용성 향상

### **✅ 완료된 Service 작업**
#### **CalendarService 인터페이스:**
```java
- getMonthlySchedules() : 월별 일정 조회 (반복 인스턴스 포함)
- getScheduleDetail() : 일정 상세 조회
- createSchedule() : 일정 생성
- updateSchedule() : 일정 부분 수정 (PATCH 방식)
- deleteSchedule() : 논리적 삭제
```

#### **RecurrenceService (반복 일정 전용):**
```java
- generateMonthlyInstances() : 월별 반복 인스턴스 생성
- generateInstancesInRange() : 기간별 반복 인스턴스 생성
- validateRRule() : RRule 문자열 검증
- getNextOccurrence() : 다음 발생 시간 계산
```

#### **RRuleUtils (유틸리티):**
```java
- isValidRRule() : RRule 검증
- generateRecurrenceInstances() : 반복 인스턴스 생성
- generateMonthlyInstances() : 월별 편의 메서드
- Examples 클래스 : 테스트용 RRule 예시들
```

### **🔧 기술적 해결사항:**
- ✅ **ical4j 4.1.1** 라이브러리 성공적 통합
- ✅ **Generic API** 활용으로 타입 안전성 확보
- ✅ **LocalDateTime** 직접 사용으로 변환 로직 단순화
- ✅ **RFC 5545 RRule** 표준 완전 준수

### **🎯 핵심 설계 결정: 반복 일정 처리 방식**

#### **반복 일정 ID 체계 - virtualId 도입**

**배경:**
- 반복 일정은 DB에 마스터 이벤트 1개만 저장하고 인스턴스는 동적 계산
- 프론트엔드에서 개별 인스턴스 클릭/수정 시 마스터 이벤트 식별 필요
- 기존 `originalScheduleId` 방식 대신 더 유연한 `virtualId` 방식 채택

**virtualId 규칙:**
```java
// 일반 일정: null
// 반복 인스턴스: "recurring-{scheduleId}-{timestamp}"
// 예시: "recurring-123-1707134400"
```

**장점:**
1. **프론트엔드 편의성**
   - 파싱하여 마스터 이벤트 ID와 발생 시점 둘 다 추출 가능
   - 개별 인스턴스 고유 식별자로 활용
   
2. **확장성**
   - 향후 개별 인스턴스 예외 처리 시 timestamp로 정확한 식별
   - 드래그&드롭, 개별 삭제 등 고급 기능 지원 가능

3. **백엔드 단순성**
   - Repository는 마스터 이벤트만 조회
   - virtualId는 Service 계층에서 동적 생성

**Repository 수정사항:**
```java
// ScheduleRepository - originalScheduleId 제거, virtualId는 Service에서 처리
"s.id, s.title, s.startAt, s.endAt, s.isAllDay, s.color, " +
"s.isRecurring, c.name, null) "  // null = virtualId는 Service에서 생성
```

**DTO 수정사항:**
- `ScheduleMonthlyItemDto.originalScheduleId` → `virtualId`로 변경
- 타입: `Long` → `String`

## 📋 **Phase 3 완료 현황 - Controller 및 Category 시스템**

### **✅ CalendarController 완전 구현**
#### **5개 Calendar API 완료:**
- `GET /api/calendars?year={year}&month={month}` - 월별 일정 조회 (반복 일정 인스턴스 포함)
- `GET /api/calendars/{id}` - 일정 상세 조회
- `POST /api/calendars` - 일정 생성
- `PATCH /api/calendars/{id}` - 일정 부분 수정
- `DELETE /api/calendars/{id}` - 일정 논리적 삭제

#### **구현 특징:**
- ✅ JWT 인증: `@AuthenticationPrincipal AuthenticatedMemberDto` 적용
- ✅ 실제 비즈니스 로직 연결: CalendarService 호출
- ✅ 포괄적 예외 처리: try-catch + 상세 에러 응답
- ✅ ApiResponse 정적 메서드 활용: success(), created(), error()
- ✅ HttpStatus 적절한 사용: 200, 201, 401, 500
- ✅ 디버깅 출력: System.out.println으로 성공/실패 추적

### **✅ Category(태그) 시스템 완전 구현**
#### **4개 Category API 완료:**
- `GET /api/tags` - 카테고리 목록 조회
- `POST /api/tags` - 카테고리 생성
- `PUT /api/tags/{id}` - 카테고리 수정
- `DELETE /api/tags/{id}` - 카테고리 삭제

#### **구현 구조:**
- ✅ **CategoryRequestDto**: Bean Validation 적용 (name 필수, HEX 색상 패턴)
- ✅ **CategoryService & CategoryServiceImpl**: CRUD + 중복체크 + 권한체크
- ✅ **CategoryController**: 완전한 에러 처리 (401, 403, 404, 409, 500)

#### **비즈니스 로직:**
- ✅ 중복 카테고리명 방지
- ✅ 사용자별 권한 체크 (본인 카테고리만 수정/삭제 가능)
- ✅ 논리적 삭제 구현
- ✅ Entity Member 관계 추가로 데이터 무결성 확보

### **🔧 최종 완성 시스템:**
**Calendar + Category 통합 시스템으로 완전한 일정 관리 기능 제공:**
1. 사용자별 카테고리 생성/관리
2. 카테고리 기반 일정 생성
3. RFC 5545 RRule 기반 반복 일정 처리
4. virtualId를 통한 반복 인스턴스 구분
5. 월별 조회에서 일반 일정 + 반복 인스턴스 통합 표시

## 🚀 진행 현황
- [x] Entity 설계 및 구현 완료
- [x] 개발 계획 수립 및 추가 제안사항 정리
- [x] Phase 1: RRule 라이브러리 추가 및 Entity 개선
- [x] **Phase 2-1: DTO 설계 및 Service 인터페이스 완료**
- [x] **Phase 2-2: RecurrenceService 및 RRuleUtils 완료**
- [x] **Phase 2-3: Repository 및 CalendarServiceImpl 구현**
- [x] **Phase 3: Controller 구현 완료**
- [x] **Phase 3+: Category(태그) 시스템 구현 완료**
- [ ] Phase 4: 알림 시스템 기초 구현

---
*이 계획을 순차적으로 진행하여 안정적이고 확장 가능한 캘린더 시스템을 구축합니다.*