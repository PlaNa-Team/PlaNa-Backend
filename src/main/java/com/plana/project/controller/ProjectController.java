package com.plana.project.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.project.dto.ProjectCreateRequestDto;
import com.plana.project.dto.ProjectCreateResponseDto;
import com.plana.project.entity.Project;
import com.plana.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> addProject(@AuthenticationPrincipal AuthenticatedMemberDto authMember,
                                                          @RequestBody @Valid ProjectCreateRequestDto requestDto) {
        Long memberId = authMember.getId();

        ProjectCreateResponseDto responseDto = projectService.createProject(requestDto, memberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "status", 201,
                        "message", "프로젝트가 등록되었습니다.",
                        "data", responseDto
                ));
    }
}
