package com.plana.calendar.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.calendar.dto.request.MemoInsertRequestDto;
import com.plana.calendar.dto.request.MemoUpdateRequestDto;
import com.plana.calendar.dto.response.ApiResponse;
import com.plana.calendar.dto.response.MemoDetailResponseDto;
import com.plana.calendar.dto.response.MemoMonthlyItemDto;
import com.plana.calendar.dto.response.MemoMonthlyResponseDto;
import com.plana.calendar.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 캘린더의 메모 관련 REST API Controller
 *
 * 구현 API 목록:
 * - GET /api/memos?year={year}&month={month}&type={type} : 월별 메모 조회 (type: 다이어리, 스케줄)
 * - POST /api/memos : 메모 생성
 * - PATCH /api/memos/{id} : 메모 수정
 */
@Slf4j
@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController {
    
    private final MemoService memoService;
    
    /**
     * 월별 메모 조회 API
     * ISO 표준 주차 번호를 기반으로 해당 월의 메모들을 조회
     * 
     * @param year 조회할 연도 (예: 2025)
     * @param month 조회할 월 (1-12)
     * @param type 메모 타입 ("다이어리" 또는 "스케줄")
     * @return 월별 메모 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MemoMonthlyResponseDto>> getMonthlyMemos(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam String type,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("GET /api/memos - year: {}, month: {}, type: {}, memberId: {}", 
                year, month, type, authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "로그인이 필요합니다.", "UNAUTHORIZED"));
            }

            System.out.println("GET /api/memos - year: " + year + ", month: " + month + ", type: " + type + ", memberId: ");
            // MemoService를 통해 월별 메모 조회
            List<MemoMonthlyItemDto> memos = memoService.getMonthlyMemos(authMember.getId(), year, month, type);
            
            // 응답 데이터 구성
            MemoMonthlyResponseDto responseData = new MemoMonthlyResponseDto(year, month, type, memos);
            
            log.info("GET /api/memos - 월별 메모 조회 성공: {}개 메모", memos.size());
            return ResponseEntity.ok(
                ApiResponse.success("메모 목록 조회 성공", responseData)
            );
            
        } catch (IllegalArgumentException e) {
            log.error("월별 메모 조회 - 입력 검증 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "입력 데이터를 확인해주세요. " + e.getMessage(), "VALIDATION_ERROR"));
                
        } catch (Exception e) {
            log.error("월별 메모 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", "INTERNAL_SERVER_ERROR"));
        }
    }
    
    /**
     * 메모 생성 API
     * 
     * @param createDto 메모 생성 요청 정보
     * @return 생성된 메모 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MemoDetailResponseDto>> createMemo(
            @RequestBody MemoInsertRequestDto createDto,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("POST /api/memos - 메모 생성: content={}, year={}, week={}, type={}, memberId: {}", 
                createDto.getContent(), createDto.getYear(), createDto.getWeek(), createDto.getType(),
                authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "로그인이 필요합니다.", "UNAUTHORIZED"));
            }
            
            // MemoService를 통해 메모 생성
            MemoDetailResponseDto createdMemo = memoService.createMemo(createDto, authMember.getId());
            
            log.info("POST /api/memos - 메모 생성 성공: id={}", createdMemo.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("메모 등록 성공", createdMemo));
                
        } catch (IllegalArgumentException e) {
            log.error("메모 생성 - 입력 검증 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "입력 데이터를 확인해주세요. " + e.getMessage(), "VALIDATION_ERROR"));
                
        } catch (SecurityException e) {
            log.error("메모 생성 - 권한 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "이 메모를 등록할 권한이 없습니다.", "FORBIDDEN"));
                
        } catch (Exception e) {
            log.error("메모 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "메모 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", "INTERNAL_SERVER_ERROR"));
        }
    }
    
    /**
     * 메모 수정 API (부분 수정)
     * 
     * @param id 수정할 메모 ID
     * @param updateDto 수정 정보
     * @return 수정된 메모 정보
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MemoDetailResponseDto>> updateMemo(
            @PathVariable Long id,
            @RequestBody MemoUpdateRequestDto updateDto,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("PATCH /api/memos/{} - 메모 수정, memberId: {}", id, 
                authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "로그인이 필요합니다.", "UNAUTHORIZED"));
            }
            
            // MemoService를 통해 메모 수정
            MemoDetailResponseDto updatedMemo = memoService.updateMemo(id, updateDto, authMember.getId());
            
            log.info("PATCH /api/memos/{} - 메모 수정 성공: content={}", id, updatedMemo.getContent());
            return ResponseEntity.ok(
                ApiResponse.success("메모 수정 성공", updatedMemo)
            );
            
        } catch (IllegalArgumentException e) {
            log.error("메모 수정 - 입력 검증 오류: {}", e.getMessage());
            
            // 메모를 찾을 수 없는 경우와 다른 검증 오류 구분
            if (e.getMessage().contains("수정할 메모를 찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "수정할 메모를 찾을 수 없습니다.", "NOT_FOUND"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "입력 데이터를 확인해주세요. " + e.getMessage(), "VALIDATION_ERROR"));
            }
            
        } catch (SecurityException e) {
            log.error("메모 수정 - 권한 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "이 메모를 수정할 권한이 없습니다.", "FORBIDDEN"));
                
        } catch (Exception e) {
            log.error("메모 수정 중 오류 발생 (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "메모 수정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", "INTERNAL_SERVER_ERROR"));
        }
    }
}
