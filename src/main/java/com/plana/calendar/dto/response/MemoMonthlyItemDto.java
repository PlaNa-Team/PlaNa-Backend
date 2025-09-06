package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 월별 메모 목록의 개별 아이템 DTO
 * 
 * 사용 API: GET /api/memos?year={year}&month={month}&type={type}
 * 사용 위치: MemoMonthlyResponseDto.memos 내부
 * 사용 Service: MemoService.getMonthlyMemos()
 * 
 * 주의사항:
 * - 월별 조회에서는 간략한 정보만 포함 (상세 조회와 차별화)
 * - ISO 표준 주차 번호(1-53) 기반으로 조회된 메모들
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoMonthlyItemDto {
    private Long id;
    private int year;
    private int week;
    private String type;
    private String content;
    private LocalDateTime createdAt;
}