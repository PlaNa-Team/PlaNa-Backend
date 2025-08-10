package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 생성 응답 DTO
 * 
 * 사용 API: POST /api/calendars
 * 사용 Service: CalendarService.createSchedule()
 * 
 * 의존 DTO:
 * - CategoryResponseDto: 카테고리 정보 (category 필드)
 * - ScheduleAlarmResponseDto: 알림 정보 (alarms 필드)
 * 
 * 주의사항:
 * - README.md 예시에서는 alarm이 단수 객체로 표시되어 있지만, 다중 알림 지원을 위해 alarms 배열로 설계
 * - 사용자 수정에 따라 category 객체를 포함하도록 변경됨
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCreateResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean isAllDay;
    private String color;
    private Boolean isRecurring;
    private String recurrenceRule;
    private LocalDateTime recurrenceUntil;
    private LocalDateTime createdAt;
    
    // 카테고리 정보
    private CategoryResponseDto category; // api 명세서에는 categoryId 만 응답한다고 했었지만, category 정보를 담아서 return 하도록 수정
    
    // 알림 정보
    private List<ScheduleAlarmResponseDto> alarms;
}