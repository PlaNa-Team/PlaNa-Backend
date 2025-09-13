package com.plana.calendar.service;

import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.calendar.dto.request.CategoryRequestDto;
import com.plana.calendar.dto.response.CategoryResponseDto;
import com.plana.calendar.entity.Category;
import com.plana.calendar.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<CategoryResponseDto> getCategoriesByMember(Long memberId) {
        log.debug("카테고리 목록 조회 - memberId: {}", memberId);
        return categoryRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto, Long memberId) {
        log.debug("카테고리 생성 - name: {}, memberId: {}", requestDto.getName(), memberId);
        
        // 사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 중복 카테고리명 체크
        Boolean exists = categoryRepository.existsByMemberIdAndName(memberId, requestDto.getName());
        if (exists) {
            throw new RuntimeException("이미 존재하는 태그입니다.");
        }
        
        // 카테고리 생성
        Category category = Category.builder()
                .name(requestDto.getName())
                .color(requestDto.getColor())
                .member(member)
                .build();
        
        Category savedCategory = categoryRepository.save(category);
        
        log.info("카테고리 생성 완료 - id: {}, name: {}", savedCategory.getId(), savedCategory.getName());
        
        return new CategoryResponseDto(
                savedCategory.getId(),
                savedCategory.getName(),
                savedCategory.getColor()
        );
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto requestDto, Long memberId) {
        log.debug("카테고리 수정 - id: {}, name: {}, memberId: {}", categoryId, requestDto.getName(), memberId);
        
        // 카테고리 조회 및 권한 체크
        Category category = categoryRepository.findByIdAndMemberId(categoryId, memberId)
                .orElseThrow(() -> {
                    // 카테고리가 존재하지 않거나 권한이 없는 경우
                    if (categoryRepository.existsById(categoryId)) {
                        return new RuntimeException("이 태그를 수정할 권한이 없습니다.");
                    } else {
                        return new RuntimeException("수정할 태그를 찾을 수 없습니다.");
                    }
                });
        
        // 중복 카테고리명 체크 (자기 자신 제외)
        Boolean exists = categoryRepository.existsByMemberIdAndNameExcludingId(memberId, requestDto.getName(), categoryId);
        if (exists) {
            throw new RuntimeException("이미 존재하는 태그입니다.");
        }
        
        // 카테고리 정보 수정
        category.setName(requestDto.getName());
        category.setColor(requestDto.getColor());
        
        Category updatedCategory = categoryRepository.save(category);
        
        log.info("카테고리 수정 완료 - id: {}, name: {}", updatedCategory.getId(), updatedCategory.getName());
        
        return new CategoryResponseDto(
                updatedCategory.getId(),
                updatedCategory.getName(),
                updatedCategory.getColor()
        );
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, Long memberId) {
        log.debug("카테고리 삭제 - id: {}, memberId: {}", categoryId, memberId);
        
        // 카테고리 조회 및 권한 체크
        Category category = categoryRepository.findByIdAndMemberId(categoryId, memberId)
                .orElseThrow(() -> {
                    // 카테고리가 존재하지 않거나 권한이 없는 경우
                    if (categoryRepository.existsById(categoryId)) {
                        return new RuntimeException("이 태그를 삭제할 권한이 없습니다.");
                    } else {
                        return new RuntimeException("삭제할 태그를 찾을 수 없습니다.");
                    }
                });
        
        // 논리적 삭제
        category.setIsDeleted(true);
        categoryRepository.save(category);
        
        log.info("카테고리 삭제 완료 - id: {}, name: {}", categoryId, category.getName());
    }
}