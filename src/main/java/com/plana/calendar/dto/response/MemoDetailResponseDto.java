package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 메모 생성/수정 응답 DTO
 * 
 * 사용 API: 
 * - POST /api/memos (메모 생성)
 * - PATCH /api/memos/{id} (메모 수정)
 * 
 * 사용 Service: 
 * - MemoService.createMemo()
 * - MemoService.updateMemo()
 * 
 * 참고:
 * - 생성/수정 후 상세 정보를 반환
 * - updatedAt은 수정 응답에서만 의미를 가짐
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoDetailResponseDto {
    private Long id;
    private String content;
    private int year;
    private int week;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}