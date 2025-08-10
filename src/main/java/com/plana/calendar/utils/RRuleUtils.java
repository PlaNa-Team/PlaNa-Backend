package com.plana.calendar.utils;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ical4j를 사용한 RRule(RFC 5545) 처리 유틸리티 클래스
 * 검증된 Google 표준 라이브러리를 사용하여 안정적인 반복 일정 처리 제공
 */
public class RRuleUtils {
    
    /**
     * RRule 문자열의 유효성 검증
     */
    public static boolean isValidRRule(String rrule) {
        if (rrule == null || rrule.trim().isEmpty()) {
            return false;
        }
        
        try {
            new RRule(rrule);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 주어진 기간 내의 반복 일정 인스턴스들을 생성
     */
    public static List<LocalDateTime> generateRecurrenceInstances(
            String rrule, 
            LocalDateTime startDateTime,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int maxInstances) {
        
        List<LocalDateTime> instances = new ArrayList<>();
        
        if (!isValidRRule(rrule)) {
            return instances;
        }
        
        try {
            RRule rRuleProperty = new RRule(rrule);
            Recur<LocalDateTime> recur = rRuleProperty.getRecur();
            
            // ical4j 4.x는 LocalDateTime을 직접 사용
            LocalDateTime current = startDateTime;
            int count = 0;
            
            // 첫 번째 인스턴스 추가 (시작 시간이 범위 내에 있으면)
            if (!current.isBefore(rangeStart) && !current.isAfter(rangeEnd)) {
                instances.add(current);
                count++;
            }
            
            // 다음 인스턴스들 생성
            while (count < maxInstances) {
                // 현재 시간에서 1초 추가하여 다음 발생 시간 찾기
                LocalDateTime nextSearchTime = current.plusSeconds(1);
                LocalDateTime nextOccurrence = recur.getNextDate(startDateTime, nextSearchTime);
                
                if (nextOccurrence == null || nextOccurrence.isAfter(rangeEnd)) {
                    break;
                }
                
                // 범위 체크
                if (!nextOccurrence.isBefore(rangeStart) && !nextOccurrence.isAfter(rangeEnd)) {
                    instances.add(nextOccurrence);
                    count++;
                }
                
                current = nextOccurrence;
            }
            
        } catch (Exception e) {
            System.err.println("RRule 처리 오류: " + rrule + ", Error: " + e.getMessage());
        }
        
        return instances;
    }
    
    /**
     * 월별 반복 일정 인스턴스 생성 (편의 메서드)
     */
    public static List<LocalDateTime> generateMonthlyInstances(
            String rrule,
            LocalDateTime startDateTime,
            int year,
            int month) {
        
        LocalDateTime rangeStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime rangeEnd = rangeStart.plusMonths(1).minusSeconds(1);
        
        return generateRecurrenceInstances(rrule, startDateTime, rangeStart, rangeEnd, 100);
    }
    
    /**
     * RRule 예시들 (개발/테스트용)
     */
    public static class Examples {
        public static final String DAILY = "FREQ=DAILY";
        public static final String WEEKLY_MONDAY = "FREQ=WEEKLY;BYDAY=MO";
        public static final String MONTHLY_THIRD_SATURDAY = "FREQ=MONTHLY;BYDAY=3SA";
        public static final String YEARLY = "FREQ=YEARLY";
        public static final String EVERY_TWO_WEEKS = "FREQ=WEEKLY;INTERVAL=2";
        public static final String WORKDAYS = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR";
        public static final String WEEKENDS = "FREQ=WEEKLY;BYDAY=SA,SU";
    }
}