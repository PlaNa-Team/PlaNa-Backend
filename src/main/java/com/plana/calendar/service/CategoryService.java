package com.plana.calendar.service;

import com.plana.calendar.dto.request.CategoryRequestDto;
import com.plana.calendar.dto.response.CategoryResponseDto;

import java.util.List;

/**
 * 카테고리(태그) 서비스 인터페이스
 * 카테고리 CRUD 및 관리 기능 정의
 */
public interface CategoryService {
    
    /**
     * 사용자별 카테고리(태그) 목록 조회
     * 
     * @param memberId 사용자 ID
     * @return 카테고리 목록
     */
    List<CategoryResponseDto> getCategoriesByMember(Long memberId);
    
    /**
     * 카테고리(태그) 생성
     * 
     * @param requestDto 카테고리 생성 요청 정보
     * @param memberId 생성하는 사용자 ID
     * @return 생성된 카테고리 정보
     */
    CategoryResponseDto createCategory(CategoryRequestDto requestDto, Long memberId);
    
    /**
     * 카테고리(태그) 수정
     * 
     * @param categoryId 수정할 카테고리 ID
     * @param requestDto 수정 정보
     * @param memberId 수정하는 사용자 ID (권한 체크용)
     * @return 수정된 카테고리 정보
     */
    CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto requestDto, Long memberId);
    
    /**
     * 카테고리(태그) 삭제 (논리적 삭제)
     * 
     * @param categoryId 삭제할 카테고리 ID
     * @param memberId 삭제하는 사용자 ID (권한 체크용)
     */
    void deleteCategory(Long categoryId, Long memberId);
}