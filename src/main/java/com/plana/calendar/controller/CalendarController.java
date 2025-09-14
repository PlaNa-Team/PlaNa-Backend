package com.plana.calendar.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.calendar.dto.request.ScheduleCreateRequestDto;
import com.plana.calendar.dto.request.ScheduleUpdateRequestDto;
import com.plana.calendar.dto.response.*;
import com.plana.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            
            // CalendarService를 통해 월별 일정 조회, monthly items 를 가져옴
            List<ScheduleMonthlyItemDto> schedules = calendarService.getMonthlySchedules(authMember.getId(), year, month);

            // monthly items 를 year, month 와 함께 담아줌. ( 기존 방식은 schedules 를 그대로 data에 전달해주면 되는거였음, 이렇게 구현하면 캐싱 처리등이 가능 )
            ScheduleMonthlyResponseDto responseData = new ScheduleMonthlyResponseDto(
                    year,
                    month,
                    schedules
            );
            
            System.out.println("GET /api/calendars - 월별 일정 조회 성공: " + schedules.size() + "개 일정");
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
            
            // CalendarService를 통해 일정 상세 조회
            ScheduleDetailResponseDto schedule = calendarService.getScheduleDetail(id, authMember.getId());
            
            System.out.println("GET /api/calendars/" + id + " - 일정 상세 조회 성공: " + schedule.getTitle());
            return ResponseEntity.ok(
                ApiResponse.success("일정 상세 조회 성공", schedule)
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
        System.out.println("POST /api/calendars - 일정 생성 시도: " + createDto.getTitle() + ", memberId: " + (authMember != null ? authMember.getId() : "null"));
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "인증이 필요합니다."));
            }

            // CalendarService를 통해 일정 생성
            ScheduleDetailResponseDto createdSchedule = calendarService.createSchedule(createDto, authMember.getId());
            
            System.out.println("POST /api/calendars - 일정 생성 성공: " + createdSchedule.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("일정 생성 성공", createdSchedule));
                
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
            
            // CalendarService를 통해 일정 수정
            ScheduleDetailResponseDto updatedSchedule = calendarService.updateSchedule(id, updateDto, authMember.getId());
            
            System.out.println("PATCH /api/calendars/" + id + " - 일정 수정 성공: " + updatedSchedule.getTitle());
            return ResponseEntity.ok(
                ApiResponse.success("일정 수정 성공", updatedSchedule)
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
            
            // CalendarService를 통해 일정 삭제
            calendarService.deleteSchedule(id, authMember.getId());
            
            System.out.println("DELETE /api/calendars/" + id + " - 일정 삭제 성공");
            return ResponseEntity.ok(
                ApiResponse.success("일정 삭제 성공", null)
            );
            
        } catch (Exception e) {
            log.error("일정 삭제 중 오류 발생 (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "일정 삭제 중 오류가 발생했습니다."));
        }
    }

    @GetMapping(params = "keyword")
    public ResponseEntity<?> searchCalendars(
            @AuthenticationPrincipal AuthenticatedMemberDto auth,
            @RequestParam String keyword
    ) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", 401,
                    "error", "UNAUTHORIZED",
                    "message", "로그인이 필요합니다."
            ));
        }

        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "error", "VALIDATION_ERROR",
                    "message", "검색어를 입력해주세요.",
                    "details", List.of(
                            Map.of("field", "keyword", "message", "검색 키워드는 공백일 수 없습니다.")
                    )
            ));
        }

        List<ScheduleSearchResponseDto> list = calendarService.search(auth.getId(), q);
        String msg = list.isEmpty() ? "검색된 일정이 없습니다." : "일정 검색 성공";

//        log.info("keyword = {}", keyword);


        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", msg,
                "data", list
        ));
    }
}
