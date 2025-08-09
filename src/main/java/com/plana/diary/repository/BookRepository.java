package com.plana.diary.repository;

import com.plana.diary.entity.Book;
import com.plana.diary.entity.Daily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByDiary_Id(Long diaryId);

    List<Book> findByDiary_IdIn(List<Long> diaryIds);
}
