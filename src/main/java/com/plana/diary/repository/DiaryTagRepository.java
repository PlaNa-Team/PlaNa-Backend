package com.plana.diary.repository;

import com.plana.diary.entity.Book;
import com.plana.diary.entity.DiaryTag;
import com.plana.diary.enums.TagStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, Long> {
    // diaryId + memberId로 태그 정보 조회
    List<DiaryTag> findByDiary_IdAndMember_Id(Long diaryId, Long memberId);

    // 다이어리 전체 태그 조회
    List<DiaryTag> findByDiary_Id(Long diaryId);

    //내가 '수락'한 태그 중 해당 날짜의 '가장 최근 수락(acceptedAt)' 1개
    Optional<DiaryTag> findTopByMember_IdAndTagStatusAndDiary_DiaryDateOrderByAcceptedAtDesc(
            Long memberId, TagStatus tagStatus, LocalDate diaryDate);

    // 같은 날짜에 내가 수락했던 다른 태그는 일괄 '거절' + acceptedAt 비움
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DiaryTag t
           set t.tagStatus = com.plana.diary.enums.TagStatus.REJECTED,
               t.acceptedAt = null
         where t.member.id = :memberId
           and t.tagStatus = com.plana.diary.enums.TagStatus.ACCEPTED
           and t.diary.diaryDate = :diaryDate
    """)
    int rejectAcceptedTagsOnDate(@Param("memberId") Long memberId,
                                 @Param("diaryDate") LocalDate diaryDate);


    List<DiaryTag> findByDiary_IdInAndMember_IdAndTagStatus(
            List<Long> diaryIds, Long memberId, TagStatus tagStatus);

    // 1) 벌크 삭제 (DELETE가 먼저 DB에 반영되도록)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DiaryTag t where t.diary.id = :diaryId")
    void deleteByDiaryId(@Param("diaryId") Long diaryId);

    // 2) 존재 여부 확인
    boolean existsByDiary_IdAndMember_Id(Long diaryId, Long memberId);
}
