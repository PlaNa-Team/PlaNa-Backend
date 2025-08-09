package com.plana.calendar.service;

import com.plana.calendar.entity.Schedule;
import com.plana.calendar.utils.RRuleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 반복 일정 처리 서비스 구현체
 * ical4j 기반으로 안정적인 반복 일정 처리
 */
@Slf4j
@Service
public class RecurrenceServiceImpl implements RecurrenceService {
    
    @Override
    public List<RecurrenceInstance> generateMonthlyInstances(Schedule schedule, int year, int month) {
        if (!schedule.getIsRecurring() || schedule.getRecurrenceRule() == null) {
            return new ArrayList<>();
        }
        
        List<LocalDateTime> instances = RRuleUtils.generateMonthlyInstances(
            schedule.getRecurrenceRule(),
            schedule.getStartAt(),
            year,
            month
        );
        
        return convertToRecurrenceInstances(schedule, instances);
    }
    
    @Override
    public List<RecurrenceInstance> generateInstancesInRange(Schedule schedule, 
                                                           LocalDateTime rangeStart, 
                                                           LocalDateTime rangeEnd) {
        if (!schedule.getIsRecurring() || schedule.getRecurrenceRule() == null) {
            return new ArrayList<>();
        }
        
        List<LocalDateTime> instances = RRuleUtils.generateRecurrenceInstances(
            schedule.getRecurrenceRule(),
            schedule.getStartAt(),
            rangeStart,
            rangeEnd,
            500
        );
        
        return convertToRecurrenceInstances(schedule, instances);
    }
    
    @Override
    public boolean validateRRule(String rrule) {
        return RRuleUtils.isValidRRule(rrule);
    }
    
    @Override
    public LocalDateTime getNextOccurrence(Schedule schedule, LocalDateTime fromDateTime) {
        if (!schedule.getIsRecurring() || schedule.getRecurrenceRule() == null) {
            return null;
        }
        
        LocalDateTime rangeEnd = fromDateTime.plusYears(1);
        List<LocalDateTime> instances = RRuleUtils.generateRecurrenceInstances(
            schedule.getRecurrenceRule(),
            schedule.getStartAt(),
            fromDateTime,
            rangeEnd,
            1
        );
        
        return instances.isEmpty() ? null : instances.get(0);
    }
    
    /**
     * LocalDateTime 인스턴스들을 RecurrenceInstance로 변환
     */
    private List<RecurrenceInstance> convertToRecurrenceInstances(Schedule schedule, List<LocalDateTime> instances) {
        List<RecurrenceInstance> recurrenceInstances = new ArrayList<>();
        
        // 원본 일정의 지속 시간 계산
        Long durationMinutes = null;
        if (schedule.getEndAt() != null) {
            durationMinutes = ChronoUnit.MINUTES.between(schedule.getStartAt(), schedule.getEndAt());
        }
        
        for (LocalDateTime instanceStart : instances) {
            // 반복 종료일 체크
            if (schedule.getRecurrenceUntil() != null && instanceStart.isAfter(schedule.getRecurrenceUntil())) {
                break;
            }
            
            // 종료 시간 계산
            LocalDateTime instanceEnd = null;
            if (durationMinutes != null) {
                instanceEnd = instanceStart.plusMinutes(durationMinutes);
            }
            
            RecurrenceInstance instance = new RecurrenceInstance(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getColor(),
                instanceStart,
                instanceEnd,
                schedule.getIsAllDay(),
                schedule.getCategory() != null ? schedule.getCategory().getName() : null,
                schedule.getCategory() != null ? schedule.getCategory().getColor() : null
            );
            
            recurrenceInstances.add(instance);
        }
        
        log.debug("Generated {} recurrence instances for schedule {}", recurrenceInstances.size(), schedule.getId());
        return recurrenceInstances;
    }
}