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
    
    // 반복 일정인 경우, 원본 일정 ID (반복 인스턴스와 구분용)
    // 반복 일정을 원본데이터를 복사해서 db에 만들어둔다면 필요가 없음.. 그렇게 할거였다면 반복 여부를 db에 저장해둘 필요없이 처음 생성할때, 반복문으로 데이터를 생성해서 저장해두면 됐던거임.
    // 반복 일정을 그때 그때 계산해서 만들거라면, 일정 고유번호가 왜 있는건지 생각해봐야함. 그때 그때 만든 일정에도 오리지널스케줄을 연결해둘거라면, erd(entity)에 필드가 추가되어야함.
    private Long originalScheduleId;
}