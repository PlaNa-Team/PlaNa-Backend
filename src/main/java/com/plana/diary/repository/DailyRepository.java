package com.plana.diary.repository;

import com.plana.diary.entity.Daily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyRepository extends JpaRepository<Daily, Long> {
    Optional<Daily> findByDiary_Id(Long diaryId);
}
