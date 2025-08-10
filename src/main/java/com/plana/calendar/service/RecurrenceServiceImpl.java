package com.plana.calendar.service;

import com.plana.calendar.utils.RRuleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 반복 일정 처리 서비스 구현체
 * ical4j 기반으로 안정적인 반복 일정 처리
 */
@Slf4j
@Service
public class RecurrenceServiceImpl implements RecurrenceService {

    // 특정 월의 반복 일정 인스턴스들을 생성 (interface 참고)
    @Override
    public List<LocalDateTime> generateMonthlyInstances(String rrule, LocalDateTime scheduleStartAt, int year, int month) {
        if (rrule == null || rrule.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 월의 첫날과 마지막날 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        return generateInstancesInRange(rrule, scheduleStartAt, startOfMonth, endOfMonth);
    }

    // 주어진 기간 내의 반복 일정 인스턴스들을 생성 (interface 참고)
    @Override
    public List<LocalDateTime> generateInstancesInRange(String rrule, 
                                                       LocalDateTime scheduleStartAt,
                                                       LocalDateTime rangeStart, 
                                                       LocalDateTime rangeEnd) {
        if (rrule == null || rrule.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<LocalDateTime> instances = RRuleUtils.generateRecurrenceInstances(
            rrule,
            scheduleStartAt,
            rangeStart,
            rangeEnd,
            100  // 최대 100개 인스턴스
        );
        
        log.debug("Generated {} recurrence instances for RRule: {}, StartAt: {}, Range: {} to {}", 
                instances.size(), rrule, scheduleStartAt, rangeStart, rangeEnd);
        
        return instances;
    }

    // RRule 문자열의 유효성 검증 (interface 참고)
    @Override
    public boolean validateRRule(String rrule) {
        return RRuleUtils.isValidRRule(rrule);
    }

    // 반복 일정의 다음 발생 시간 계산 (interface 참고)
    @Override
    public LocalDateTime getNextOccurrence(String rrule, LocalDateTime scheduleStartAt, LocalDateTime fromDateTime) {
        if (rrule == null || rrule.trim().isEmpty()) {
            return null;
        }
        
        // 현재 시간부터 1년 뒤까지의 범위에서 다음 발생 시간 찾기
        LocalDateTime oneYearLater = fromDateTime.plusYears(1);
        
        List<LocalDateTime> instances = RRuleUtils.generateRecurrenceInstances(
            rrule,
            scheduleStartAt,
            fromDateTime,
            oneYearLater,
            1  // 첫 번째 인스턴스만 필요
        );
        
        LocalDateTime nextOccurrence = instances.isEmpty() ? null : instances.get(0);
        log.debug("Next occurrence for RRule: {} from {} is {}", rrule, fromDateTime, nextOccurrence);
        
        return nextOccurrence;
    }
}