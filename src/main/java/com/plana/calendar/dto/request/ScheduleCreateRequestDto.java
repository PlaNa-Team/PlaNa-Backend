package com.plana.calendar.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 생성 요청 DTO
 * 
 * 사용 API: POST /api/calendars
 * 사용 Service: CalendarService.createSchedule()
 * 
 * 의존 DTO:
 * - ScheduleAlarmRequestDto: 알림 설정 정보 (alarms 필드)
 * 
 * 주의사항:
 * - memberId는 실제로는 JWT 토큰에서 추출하므로 요청에서 제외 고려 필요
 * - README.md에서는 "alarm" 단수형이지만 DTO에서는 "alarms" 복수형 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCreateRequestDto {
    private Long categoryId;
    private String title;
    private String color;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean isAllDay;
    private Boolean isRecurring;
    private String recurrenceRule;
    private LocalDateTime recurrenceUntil;
    private List<ScheduleAlarmRequestDto> alarms;
}