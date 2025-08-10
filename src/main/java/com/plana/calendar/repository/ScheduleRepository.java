package com.plana.calendar.repository;

import com.plana.calendar.dto.response.ScheduleMonthlyItemDto;
import com.plana.calendar.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    // 월별 일반 일정 조회 (Entity 반환 - 반복 일정 제외)
    @Query("SELECT s FROM Schedule s " +
            "LEFT JOIN FETCH s.category " +
            "WHERE s.member.id = :memberId " +
            "AND s.isRecurring = false " +
            "AND s.isDeleted = false " +
            "AND ((s.startAt BETWEEN :start AND :end) " +
            "OR (s.endAt BETWEEN :start AND :end) " +
            "OR (s.startAt <= :start AND s.endAt >= :end)) " +
            "ORDER BY s.startAt ASC")
    List<Schedule> findNonRecurringSchedulesInRange(@Param("memberId") Long memberId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
    
    // 일정 상세 조회 (Entity 반환 - 연관관계 필요)
    @Query("SELECT s FROM Schedule s " +
            "LEFT JOIN FETCH s.category " +
            "LEFT JOIN FETCH s.alarms " +
            "WHERE s.id = :id AND s.member.id = :memberId")
    Optional<Schedule> findByIdAndMemberId(@Param("id") Long id, 
                                         @Param("memberId") Long memberId);
    
    // 반복 일정만 조회 (RRule 처리를 위해)
    @Query("SELECT s FROM Schedule s " +
            "LEFT JOIN FETCH s.category " +
            "WHERE s.member.id = :memberId " +
            "AND s.isRecurring = true " +
            "AND s.isDeleted = false " +
            "AND s.startAt <= :rangeEnd " +
            "ORDER BY s.startAt ASC")
    List<Schedule> findRecurringSchedulesForRange(@Param("memberId") Long memberId,
                                                @Param("rangeEnd") LocalDateTime rangeEnd);
    
    // 사용자별 일정 개수 (성능 확인용)
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.member.id = :memberId")
    Long countByMemberId(@Param("memberId") Long memberId);
}