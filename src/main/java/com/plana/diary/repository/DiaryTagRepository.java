package com.plana.diary.repository;

import com.plana.diary.entity.DiaryTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, Long> {
}
