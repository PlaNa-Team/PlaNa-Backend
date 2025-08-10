package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 월별 일정 목록 응답 DTO (상위 컴테이너)
 * 
 * 사용 API: GET /api/calendars?year={year}&month={month}
 * 사용 Service: CalendarService.getMonthlySchedules()
 * 
 * 의존 DTO:
 * - ScheduleMonthlyItemDto: 개별 일정 아이템 (schedules 필드)
 * 
 * 참고:
 * - 연도/월 정보와 함께 일정 목록을 구조화하여 전달
 * - 반복 일정 인스턴스도 포함하여 처리
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMonthlyResponseDto {
    private int year;
    private int month;
    private List<ScheduleMonthlyItemDto> schedules;
}