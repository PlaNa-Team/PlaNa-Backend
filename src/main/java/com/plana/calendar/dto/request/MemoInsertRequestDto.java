package com.plana.calendar.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메모 생성 요청 DTO
 * 
 * 사용 API: POST /api/memos
 * 사용 Service: MemoService.createMemo()
 * 
 * 검증 규칙:
 * - content: 필수, 1-255자
 * - year: 필수, 양수
 * - week: 필수, 1-53 범위
 * - type: 필수, "다이어리" 또는 "스케줄"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoInsertRequestDto {
    private String content;
    private int year;
    private int week;
    private String type;
}