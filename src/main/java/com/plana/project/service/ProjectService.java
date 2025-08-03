package com.plana.project.service;

import com.plana.project.dto.ProjectCreateRequestDto;
import com.plana.project.dto.ProjectCreateResponseDto;

public interface ProjectService {
    ProjectCreateResponseDto createProject(ProjectCreateRequestDto dto, Long memberId);
}
