package com.plana.diary.repository;

import com.plana.diary.entity.Daily;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRepository extends JpaRepository<Daily, Long> {
}
