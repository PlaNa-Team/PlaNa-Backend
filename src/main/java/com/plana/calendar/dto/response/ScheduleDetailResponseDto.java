package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 상세 조회 응답 DTO
 * 
 * 사용 API: GET /api/calendars/{id}
 * 사용 Service: CalendarService.getScheduleDetail()
 * 
 * 의존 DTO:
 * - CategoryResponseDto: 카테고리 정보 (category 필드)
 * - ScheduleAlarmResponseDto: 알림 정보 (alarms 필드)
 * 
 * 주의사항:
 * - README.md에서는 카테고리/알림 정보가 평면적이지만, DTO에서는 중체적 구조로 설계
 * - hasNotification 필드는 alarms 배열의 존재 여부로 계산 가능
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDetailResponseDto {
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
    private Boolean hasNotification;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 카테고리 정보
    private CategoryResponseDto category;
    
    // 알림 정보
    private List<ScheduleAlarmResponseDto> alarms;
}