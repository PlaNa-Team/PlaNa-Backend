package com.plana.project.service;

import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.project.dto.ProjectCreateRequestDto;
import com.plana.project.dto.ProjectCreateResponseDto;
import com.plana.project.entity.Project;
import com.plana.project.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ProjectCreateResponseDto createProject(ProjectCreateRequestDto dto, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Project project = Project.builder()
                .member(member)
                .title(dto.getTitle())
                .status(Project.Status.예정) // 기본값
                .year(Year.now()) // 현재 연도
                .startMonth(null)
                .endMonth(null)
                .isDeleted(false)
                .build();

        Project saved = projectRepository.save(project);

        return ProjectCreateResponseDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .status(saved.getStatus().toString())
                .year(saved.getYear().getValue())
                .startMonth(saved.getStartMonth())
                .endMonth(saved.getEndMonth())
                .build();
    }


}
