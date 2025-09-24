package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 월별 메모 목록 응답 DTO (상위 컨테이너)
 * 
 * 사용 API: GET /api/memos?year={year}&month={month}&type={type}
 * 사용 Service: MemoService.getMonthlyMemos()
 * 
 * 의존 DTO:
 * - MemoMonthlyItemDto: 개별 메모 아이템 (memos 필드)
 * 
 * 참고:
 * - 연도/월 정보와 함께 메모 목록을 구조화하여 전달
 * - ISO 표준 주차 번호 기반으로 처리된 메모들 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoMonthlyResponseDto {
    private int year;
    private int month;
    private String type;
    private List<MemoMonthlyItemDto> memos;
}