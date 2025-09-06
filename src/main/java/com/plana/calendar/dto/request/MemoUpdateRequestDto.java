package com.plana.calendar.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메모 수정 요청 DTO
 * 
 * 사용 API: PATCH /api/memos/{id}
 * 사용 Service: MemoService.updateMemo()
 * 
 * 검증 규칙:
 * - content: 선택적, 1-255자 (제공시)
 * - type: 선택적, "다이어리" 또는 "스케줄" (제공시)
 * 
 * 참고:
 * - PATCH 방식으로 부분 수정 지원
 * - year, week는 수정 불가 (생성시에만 설정)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoUpdateRequestDto {
    private String content;
    private String type;
}