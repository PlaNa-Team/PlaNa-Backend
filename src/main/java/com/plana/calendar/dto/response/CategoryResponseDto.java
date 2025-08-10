package com.plana.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카테고리 정보 응답 DTO
 * 
 * 사용 위치:
 * - ScheduleDetailResponseDto.category 필드
 * - ScheduleCreateResponseDto.category 필드
 * 
 * 연결 Entity: Category
 * 
 * 주의사항:
 * - 독립적으로 API에서 사용되지 않고, 다른 Response DTO의 중체 객체로만 사용
 * - is_deleted 필드는 Entity에만 존재하고 Response에는 포함 안함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    private Long id;
    private String name;
    private String color;
}