package com.plana.calendar.repository;

import com.plana.calendar.dto.response.ScheduleAlarmResponseDto;
import com.plana.calendar.entity.ScheduleAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleAlarmRepository extends JpaRepository<ScheduleAlarm, Long> {
    
    // 일정별 알림 조회 (DTO로 직접 반환)
    @Query("SELECT new com.plana.calendar.dto.response.ScheduleAlarmResponseDto(" +
            "sa.id, sa.notifyBeforeVal, sa.notifyUnit) " +
            "FROM ScheduleAlarm sa " +
            "WHERE sa.schedule.id = :scheduleId ")
    List<ScheduleAlarmResponseDto> findByScheduleId(@Param("scheduleId") Long scheduleId);
    
    // 일정별 알림 삭제 (CASCADE 대신 명시적 삭제)
    @Modifying
    @Query("DELETE FROM ScheduleAlarm sa WHERE sa.schedule.id = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
    
    // 특정 시간대 알림 조회 (알림 발송용), 5 분전, 1시간전 단위가 다른데 추가 고려 필요.
    @Query("SELECT sa FROM ScheduleAlarm sa " +
            "JOIN sa.schedule s " +
            "WHERE sa.notifyBeforeVal BETWEEN :start AND :end " +
            "ORDER BY sa.notifyBeforeVal ASC")
    List<ScheduleAlarm> findAlarmsInTimeRange(@Param("start") java.time.LocalDateTime start,
                                            @Param("end") java.time.LocalDateTime end);
}