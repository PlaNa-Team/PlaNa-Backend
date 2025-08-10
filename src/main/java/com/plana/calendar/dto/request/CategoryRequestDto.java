package com.plana.calendar.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 카테고리(태그) 생성/수정 요청 DTO
 * 
 * 사용 API:
 * - POST /api/tags (카테고리 생성)
 * - PUT /api/tags/{id} (카테고리 수정)
 * 
 * 유효성 검증:
 * - name: 필수, 1-50자
 * - color: HEX 색상 코드 형식 (#RRGGBB)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {
    
    @NotBlank(message = "카테고리 이름은 필수입니다.")
    @Size(min = 1, max = 50, message = "카테고리 이름은 1자 이상 50자 이하여야 합니다.")
    private String name;
    
    @NotBlank(message = "색상 코드는 필수입니다.")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", 
             message = "올바른 HEX 색상 코드여야 합니다. (예: #FF5722, #F57)")
    private String color;
}