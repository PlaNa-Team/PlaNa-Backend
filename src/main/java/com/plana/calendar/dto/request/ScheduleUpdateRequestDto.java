package com.plana.calendar.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 수정 요청 DTO
 * 
 * 사용 API: PUT /api/calendars/{id}
 * 사용 Service: CalendarService.updateSchedule()
 * 
 * 의존 DTO:
 * - ScheduleAlarmRequestDto: 알림 설정 정보 (alarms 필드)
 * 
 * 참고:
 * - README.md에서 PUT method 명시가 누락되어 있지만 비즈니스 로직으로 판단하여 추가
 * - 모든 필드는 Optional로 처리되어야 하지만 현재는 간단히 구현
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateRequestDto {
    private Long categoryId;
    private String title;
    private String description;
    private String color;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean isAllDay;
    private Boolean isRecurring;
    private String recurrenceRule;
    private LocalDateTime recurrenceUntil;
    private List<ScheduleAlarmRequestDto> alarms;
}