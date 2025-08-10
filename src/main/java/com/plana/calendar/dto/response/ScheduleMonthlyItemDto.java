package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 월별 일정 목록의 개별 아이템 DTO
 * 
 * 사용 API: GET /api/calendars?year={year}&month={month}
 * 사용 위치: ScheduleMonthlyResponseDto.schedules 내부
 * 사용 Service: CalendarService.getMonthlySchedules()
 * 
 * 주의사항:
 * - api 명세서에 color, isRecurring, categoryName 필드 추가 고려 (카테고리 구분, 반복 여부 확인을 위해서)
 * - originalScheduleId는 반복 일정 인스턴스 구분용 (일반 일정에서는 null)
 * - 월별 조회에서는 간략한 정보만 포함 (상세 조회와 차별화)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMonthlyItemDto {
    private Long id;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean isAllDay;
    private String color;
    private Boolean isRecurring;
    private String categoryName;
    
    /**
     * 가상 ID (Virtual ID)
     * 
     * 일반 일정: null
     * 반복 인스턴스: "recurring-{scheduleId}-{timestamp}"
     * 
     * 사용 목적:
     * - 프론트엔드에서 개별 반복 인스턴스 식별 및 수정
     * - 마스터 이벤트 ID와 발생 시점을 동시에 전달
     * - 향후 개별 인스턴스 예외 처리 지원 (예외 처리를 저장해둘 db 컬럼이 추가로 필요할 수 있음)
     * 
     * 예시:
     * - 일반 일정: virtualId = null
     * - 반복 인스턴스: virtualId = "recurring-123-1707134400"
     */
    private String virtualId;
}