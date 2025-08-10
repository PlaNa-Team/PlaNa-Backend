package com.plana.calendar.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.calendar.dto.request.ScheduleCreateRequestDto;
import com.plana.calendar.dto.request.ScheduleUpdateRequestDto;
import com.plana.calendar.dto.response.ApiResponse;
import com.plana.calendar.dto.response.ScheduleMonthlyResponseDto;
import com.plana.calendar.dto.response.ScheduleDetailResponseDto;
import com.plana.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

/**
 * 캘린더 관련 REST API Controller
 * 
 * 구현 API 목록:
 * - GET /api/calendars?year={year}&month={month} : 월별 일정 조회
 * - GET /api/calendars/{id} : 일정 상세 조회  
 * - POST /api/calendars : 일정 생성
 * - PATCH /api/calendars/{id} : 일정 수정
 * - DELETE /api/calendars/{id} : 일정 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 월별 일정 조회 API
     * 일반 일정과 반복 일정 인스턴스를 통합하여 반환
     * 
     * @param year 조회할 연도 (예: 2024)
     * @param month 조회할 월 (1-12)
     * @return 월별 일정 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ScheduleMonthlyResponseDto>> getMonthlySchedules(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("GET /api/calendars - year: {}, month: {}, memberId: {}", year, month, authMember.getId());
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "인증이 필요합니다."));
            }
            
            // 임시 응답 (null 데이터)
            ScheduleMonthlyResponseDto responseData = new ScheduleMonthlyResponseDto(
                    year,
                    month,
                    new ArrayList<>() // 빈 일정 목록
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("월별 일정 조회 성공", responseData)
            );
            
        } catch (Exception e) {
            log.error("월별 일정 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "월별 일정 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 일정 상세 조회 API
     * 
     * @param id 조회할 일정 ID
     * @return 일정 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDetailResponseDto>> getScheduleDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("GET /api/calendars/{} - 일정 상세 조회, memberId: {}", id, authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "인증이 필요합니다."));
            }
            
            return ResponseEntity.ok(
                ApiResponse.success("일정 상세 조회 성공", null)
            );
            
        } catch (Exception e) {
            log.error("일정 상세 조회 중 오류 발생 (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "일정 상세 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 일정 생성 API
     * 
     * @param createDto 일정 생성 요청 정보
     * @return 생성된 일정 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleDetailResponseDto>> createSchedule(
            @RequestBody ScheduleCreateRequestDto createDto,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("POST /api/calendars - 일정 생성: {}, memberId: {}", createDto.getTitle(), authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "인증이 필요합니다."));
            }
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("일정 생성 성공", null));
                
        } catch (Exception e) {
            log.error("일정 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "일정 생성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 일정 수정 API (부분 수정)
     * 
     * @param id 수정할 일정 ID
     * @param updateDto 수정 정보
     * @return 수정된 일정 정보
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDetailResponseDto>> updateSchedule(
            @PathVariable Long id,
            @RequestBody ScheduleUpdateRequestDto updateDto,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("PATCH /api/calendars/{} - 일정 수정, memberId: {}", id, authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "인증이 필요합니다."));
            }
            
            return ResponseEntity.ok(
                ApiResponse.success("일정 수정 성공", null)
            );
            
        } catch (Exception e) {
            log.error("일정 수정 중 오류 발생 (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "일정 수정 중 오류가 발생했습니다."));
        }
    }

    /**
     * 일정 삭제 API (논리적 삭제)
     * 
     * @param id 삭제할 일정 ID
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("DELETE /api/calendars/{} - 일정 삭제, memberId: {}", id, authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "인증이 필요합니다."));
            }
            
            return ResponseEntity.ok(
                ApiResponse.success("일정 삭제 성공", null)
            );
            
        } catch (Exception e) {
            log.error("일정 삭제 중 오류 발생 (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "일정 삭제 중 오류가 발생했습니다."));
        }
    }
}
