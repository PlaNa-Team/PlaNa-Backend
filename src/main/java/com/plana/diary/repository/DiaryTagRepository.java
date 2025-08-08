package com.plana.diary.repository;

import com.plana.diary.entity.Book;
import com.plana.diary.entity.DiaryTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, Long> {
    // diaryId + memberId로 태그 정보 조회
    List<DiaryTag> findByDiary_IdAndMember_Id(Long diaryId, Long memberId);

    // 다이어리 전체 태그 조회
    List<DiaryTag> findByDiary_Id(Long diaryId);
}
