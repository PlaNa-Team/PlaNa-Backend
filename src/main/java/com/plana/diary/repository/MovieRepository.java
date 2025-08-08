package com.plana.diary.repository;

import com.plana.diary.entity.Book;
import com.plana.diary.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByDiary_Id(Long diaryId);
}
