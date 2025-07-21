package com.plana.diary.repository;

import com.plana.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

// 제네릭 타입 2개가 들어간다. Diary는 엔티티 타입, Long은 Diary의 기본키 타입
public interface DiaryRepository extends JpaRepository<Diary, Long> {
}
