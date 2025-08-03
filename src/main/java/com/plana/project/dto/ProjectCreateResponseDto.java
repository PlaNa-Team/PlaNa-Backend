package com.plana.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectCreateResponseDto {
    private Long id;
    private String title;
    private String status;
    private int year;
    private Integer startMonth;
    private Integer endMonth;
}
