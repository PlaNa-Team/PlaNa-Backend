package com.plana.calendar.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.calendar.dto.request.CategoryRequestDto;
import com.plana.calendar.dto.response.ApiResponse;
import com.plana.calendar.dto.response.CategoryResponseDto;
import com.plana.calendar.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 카테고리(태그) 관련 REST API Controller
 * 
 * 구현 API 목록:
 * - GET /api/tags : 카테고리 목록 조회
 * - POST /api/tags : 카테고리 생성
 * - PUT /api/tags/{id} : 카테고리 수정
 * - DELETE /api/tags/{id} : 카테고리 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 카테고리(태그) 목록 조회 API
     * 
     * @param authMember 인증된 사용자 정보
     * @return 카테고리 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategories(
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("GET /api/tags - 카테고리 목록 조회, memberId: {}", 
                authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "로그인이 필요합니다."));
            }
            
            List<CategoryResponseDto> categories = categoryService.getCategoriesByMember(authMember.getId());
            
            return ResponseEntity.ok(
                ApiResponse.success("태그 목록 조회 성공", categories)
            );
            
        } catch (Exception e) {
            log.error("카테고리 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "태그 목록 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    /**
     * 카테고리(태그) 생성 API
     * 
     * @param requestDto 카테고리 생성 요청 정보
     * @param authMember 인증된 사용자 정보
     * @return 생성된 카테고리 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
            @Valid @RequestBody CategoryRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("POST /api/tags - 카테고리 생성: {}, memberId: {}", 
                requestDto.getName(), authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "로그인이 필요합니다."));
            }
            
            CategoryResponseDto createdCategory = categoryService.createCategory(requestDto, authMember.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("태그 등록 성공", createdCategory));
                
        } catch (RuntimeException e) {
            if (e.getMessage().contains("이미 존재하는 태그")) {
                log.warn("카테고리 생성 실패 - 중복: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, e.getMessage()));
            } else {
                log.error("카테고리 생성 중 오류 발생: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "태그 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
            }
        }
    }

    /**
     * 카테고리(태그) 수정 API
     * 
     * @param id 수정할 카테고리 ID
     * @param requestDto 수정 정보
     * @param authMember 인증된 사용자 정보
     * @return 수정된 카테고리 정보
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("PUT /api/tags/{} - 카테고리 수정: {}, memberId: {}", id, requestDto.getName(), authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "로그인이 필요합니다."));
            }
            
            CategoryResponseDto updatedCategory = categoryService.updateCategory(id, requestDto, authMember.getId());
            
            return ResponseEntity.ok(
                ApiResponse.success("태그 수정 성공", updatedCategory)
            );
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("카테고리 수정 실패 - 없음: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
            } else if (e.getMessage().contains("권한이 없습니다")) {
                log.warn("카테고리 수정 실패 - 권한: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, e.getMessage()));
            } else if (e.getMessage().contains("이미 존재하는 태그")) {
                log.warn("카테고리 수정 실패 - 중복: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, e.getMessage()));
            } else {
                log.error("카테고리 수정 중 오류 발생: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "태그 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
            }
        }
    }

    /**
     * 카테고리(태그) 삭제 API
     * 
     * @param id 삭제할 카테고리 ID
     * @param authMember 인증된 사용자 정보
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        log.info("DELETE /api/tags/{} - 카테고리 삭제, memberId: {}", 
                id, authMember != null ? authMember.getId() : "null");
        
        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "로그인이 필요합니다."));
            }
            
            categoryService.deleteCategory(id, authMember.getId());
            
            return ResponseEntity.ok(
                ApiResponse.success("태그 삭제 성공", null)
            );
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("카테고리 삭제 실패 - 없음: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
            } else if (e.getMessage().contains("권한이 없습니다")) {
                log.warn("카테고리 삭제 실패 - 권한: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, e.getMessage()));
            } else {
                log.error("카테고리 삭제 중 오류 발생: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "태그 삭제 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
            }
        }
    }
}