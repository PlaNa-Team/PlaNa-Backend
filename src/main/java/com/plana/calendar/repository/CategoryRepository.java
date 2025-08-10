package com.plana.calendar.repository;

import com.plana.calendar.dto.response.CategoryResponseDto;
import com.plana.calendar.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // 사용자별 카테고리 조회 (DTO로 직접 반환)
    @Query("SELECT new com.plana.calendar.dto.response.CategoryResponseDto(" +
            "c.id, c.name, c.color) " +
            "FROM Category c " +
            "WHERE c.member.id = :memberId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.name ASC")
    List<CategoryResponseDto> findByMemberId(@Param("memberId") Long memberId);
    
    // 특정 카테고리 조회 (Entity 반환 - 수정/삭제용)
    @Query("SELECT c FROM Category c " +
            "WHERE c.id = :id AND c.member.id = :memberId AND c.isDeleted = false")
    Optional<Category> findByIdAndMemberId(@Param("id") Long id, 
                                         @Param("memberId") Long memberId);
    
    // 카테고리명 중복 확인
    @Query("SELECT COUNT(c) > 0 FROM Category c " +
            "WHERE c.member.id = :memberId " +
            "AND c.name = :name " +
            "AND c.isDeleted = false")
    Boolean existsByMemberIdAndName(@Param("memberId") Long memberId, 
                                   @Param("name") String name);
    
    // 카테고리명 중복 확인 (수정시 - 자기 자신 제외)
    @Query("SELECT COUNT(c) > 0 FROM Category c " +
            "WHERE c.member.id = :memberId " +
            "AND c.name = :name " +
            "AND c.id != :excludeId " +
            "AND c.isDeleted = false")
    Boolean existsByMemberIdAndNameExcludingId(@Param("memberId") Long memberId, 
                                              @Param("name") String name,
                                              @Param("excludeId") Long excludeId);
}