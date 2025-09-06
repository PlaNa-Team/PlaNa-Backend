package com.plana.calendar.service;

import com.plana.calendar.dto.request.MemoInsertRequestDto;
import com.plana.calendar.dto.request.MemoUpdateRequestDto;
import com.plana.calendar.dto.response.MemoDetailResponseDto;
import com.plana.calendar.dto.response.MemoMonthlyItemDto;

import java.util.List;

/**
 * 메모 서비스 인터페이스
 * 메모의 CRUD 및 조회 기능 정의
 */
public interface MemoService {
    
    /**
     * 특정 월의 메모 조회 (타입별)
     * ISO 표준 주차 번호를 기반으로 해당 월에 포함되는 주차들의 메모를 조회
     * 
     * @param memberId 사용자 ID
     * @param year 조회할 연도
     * @param month 조회할 월 (1-12)
     * @param type 메모 타입 ("다이어리" 또는 "스케줄")
     * @return 월별 메모 목록
     */
    List<MemoMonthlyItemDto> getMonthlyMemos(Long memberId, int year, int month, String type);
    
    /**
     * 메모 생성
     * 
     * @param createDto 메모 생성 요청 정보
     * @param memberId 생성하는 사용자 ID
     * @return 생성된 메모 정보
     */
    MemoDetailResponseDto createMemo(MemoInsertRequestDto createDto, Long memberId);
    
    /**
     * 메모 수정 (부분 수정)
     * 
     * @param memoId 수정할 메모 ID
     * @param updateDto 수정 정보
     * @param memberId 수정하는 사용자 ID (권한 체크용)
     * @return 수정된 메모 정보
     */
    MemoDetailResponseDto updateMemo(Long memoId, MemoUpdateRequestDto updateDto, Long memberId);
}