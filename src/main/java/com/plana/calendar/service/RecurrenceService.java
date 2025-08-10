package com.plana.calendar.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 반복 일정 처리 서비스
 * ical4j 기반으로 RFC 5545 표준을 준수하는 반복 일정 관리
 */
public interface RecurrenceService {
    
    /**
     * 특정 월의 반복 일정 인스턴스들을 생성
     * 
     * @param rrule RRule 문자열 (예: "FREQ=WEEKLY;BYDAY=TU")
     * @param scheduleStartAt 원본 일정 시작 시간
     * @param year 조회할 연도
     * @param month 조회할 월 (1-12)
     * @return 해당 월에 포함되는 반복 인스턴스의 시작 시간들
     */
    List<LocalDateTime> generateMonthlyInstances(String rrule, LocalDateTime scheduleStartAt, int year, int month);
    
    /**
     * 주어진 기간 내의 반복 일정 인스턴스들을 생성
     * 
     * @param rrule RRule 문자열 (예: "FREQ=WEEKLY;BYDAY=TU")
     * @param scheduleStartAt 원본 일정 시작 시간
     * @param rangeStart 조회 범위 시작
     * @param rangeEnd 조회 범위 종료
     * @return 해당 범위에 포함되는 반복 인스턴스의 시작 시간들
     */
    List<LocalDateTime> generateInstancesInRange(String rrule, 
                                               LocalDateTime scheduleStartAt,
                                               LocalDateTime rangeStart, 
                                               LocalDateTime rangeEnd);
    
    /**
     * RRule 문자열의 유효성 검증
     * 
     * @param rrule 검증할 RRule 문자열
     * @return 유효한 경우 true
     */
    boolean validateRRule(String rrule);
    
    /**
     * 반복 일정의 다음 발생 시간 계산
     * 
     * @param rrule RRule 문자열
     * @param scheduleStartAt 원본 일정 시작 시간
     * @param fromDateTime 기준 시간 (이후 첫 번째 발생 시간 조회)
     * @return 다음 발생 시간 (없으면 null)
     */
    LocalDateTime getNextOccurrence(String rrule, LocalDateTime scheduleStartAt, LocalDateTime fromDateTime);
}