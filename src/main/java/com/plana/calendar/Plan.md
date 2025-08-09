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
6. **DTO 클래스 생성**
   - Request/Response DTO 구조화
   - 기존 프로젝트 패턴 준수

7. **Controller 구현**
   - `GET /api/calendars?year=2025&month=6` (월별 조회)
   - `GET /api/calendars/{id}` (상세 조회)
   - `POST /api/calendars` (생성)
   - `PUT /api/calendars/{id}` (수정)
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

## 🚀 진행 현황
- [x] Entity 설계 및 구현 완료
- [x] 개발 계획 수립 및 추가 제안사항 정리
- [ ] Phase 1: RRule 라이브러리 추가 및 Entity 개선
- [ ] Phase 2: Repository 및 Service 구현
- [ ] Phase 3: DTO 및 Controller 구현
- [ ] Phase 4: 알림 시스템 기초 구현

---
*이 계획을 순차적으로 진행하여 안정적이고 확장 가능한 캘린더 시스템을 구축합니다.*