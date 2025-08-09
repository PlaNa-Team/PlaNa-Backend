package com.plana.calendar.service;

import com.plana.calendar.entity.Schedule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 반복 일정 처리 서비스
 * ical4j 기반으로 RFC 5545 표준을 준수하는 반복 일정 관리
 */
public interface RecurrenceService {
    
    /**
     * 특정 월의 반복 일정 인스턴스들을 생성
     */
    List<RecurrenceInstance> generateMonthlyInstances(Schedule schedule, int year, int month);
    
    /**
     * 주어진 기간 내의 반복 일정 인스턴스들을 생성
     */
    List<RecurrenceInstance> generateInstancesInRange(Schedule schedule, 
                                                     LocalDateTime rangeStart, 
                                                     LocalDateTime rangeEnd);
    
    /**
     * RRule 문자열의 유효성 검증
     */
    boolean validateRRule(String rrule);
    
    /**
     * 반복 일정의 다음 발생 시간 계산
     */
    LocalDateTime getNextOccurrence(Schedule schedule, LocalDateTime fromDateTime);
    
    /**
     * 반복 일정 인스턴스 정보
     */
    class RecurrenceInstance {
        private final Long originalScheduleId;
        private final String title;
        private final String description;
        private final String color;
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;
        private final Boolean isAllDay;
        private final String categoryName;
        private final String categoryColor;
        
        public RecurrenceInstance(Long originalScheduleId, String title, String description, 
                                String color, LocalDateTime startAt, LocalDateTime endAt, 
                                Boolean isAllDay, String categoryName, String categoryColor) {
            this.originalScheduleId = originalScheduleId;
            this.title = title;
            this.description = description;
            this.color = color;
            this.startAt = startAt;
            this.endAt = endAt;
            this.isAllDay = isAllDay;
            this.categoryName = categoryName;
            this.categoryColor = categoryColor;
        }
        
        // Getters
        public Long getOriginalScheduleId() { return originalScheduleId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getColor() { return color; }
        public LocalDateTime getStartAt() { return startAt; }
        public LocalDateTime getEndAt() { return endAt; }
        public Boolean getIsAllDay() { return isAllDay; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryColor() { return categoryColor; }
    }
}