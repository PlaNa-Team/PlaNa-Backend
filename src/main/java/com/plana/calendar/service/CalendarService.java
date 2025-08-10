package com.plana.calendar.service;

import com.plana.calendar.dto.request.ScheduleCreateRequestDto;
import com.plana.calendar.dto.request.ScheduleUpdateRequestDto;
import com.plana.calendar.dto.response.ScheduleDetailResponseDto;
import com.plana.calendar.dto.response.ScheduleMonthlyItemDto;

import java.util.List;

/**
 * 캘린더 서비스 인터페이스
 * 일정의 CRUD 및 조회 기능 정의
 */
public interface CalendarService {
    
    /**
     * 특정 월의 모든 일정 조회 (일반 일정 + 반복 일정 인스턴스)
     * @param memberId 사용자 ID
     * @param year 조회할 연도
     * @param month 조회할 월 (1-12)
     * @return 월별 일정 목록
     */
    List<ScheduleMonthlyItemDto> getMonthlySchedules(Long memberId, int year, int month);
    
    /**
     * 일정 상세 조회
     * @param scheduleId 일정 ID
     * @param memberId 요청한 사용자 ID (권한 체크용)
     * @return 일정 상세 정보
     */
    ScheduleDetailResponseDto getScheduleDetail(Long scheduleId, Long memberId);
    
    /**
     * 일정 생성
     * @param createDto 일정 생성 요청 정보
     * @param memberId 생성하는 사용자 ID
     * @return 생성된 일정 정보
     */
    ScheduleDetailResponseDto createSchedule(ScheduleCreateRequestDto createDto, Long memberId);
    
    /**
     * 일정 수정
     * @param scheduleId 수정할 일정 ID
     * @param updateDto 수정 정보
     * @param memberId 수정하는 사용자 ID (권한 체크용)
     * @return 수정된 일정 정보
     */
    ScheduleDetailResponseDto updateSchedule(Long scheduleId, ScheduleUpdateRequestDto updateDto, Long memberId);
    
    /**
     * 일정 삭제 (논리적 삭제)
     * @param scheduleId 삭제할 일정 ID
     * @param memberId 삭제하는 사용자 ID (권한 체크용)
     */
    void deleteSchedule(Long scheduleId, Long memberId);
    
}