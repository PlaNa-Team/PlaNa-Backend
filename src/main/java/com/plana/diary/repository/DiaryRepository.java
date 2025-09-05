package com.plana.diary.repository;

import com.plana.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// 제네릭 타입 2개가 들어간다. Diary는 엔티티 타입, Long은 Diary의 기본키 타입
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    // 작성자 + 날짜범위 필터
    @Query("SELECT d FROM Diary d " +
            "WHERE d.writer.id = :writerId " + // d.writer.id : DB에서 가져온 다이어리의 작성자 id, :writerId : 내가 찾고 싶은 작성자 id
            "And d.diaryDate BETWEEN :start AND :end " +
            "ORDER BY d.diaryDate ASC"
    )
    List<Diary> findMonthlyDiaries(@Param("writerId") Long writerId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    Optional<Diary> findTopByWriter_IdAndDiaryDateOrderByCreatedAtDesc(Long writerId, LocalDate diaryDate);

}
