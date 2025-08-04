package com.plana.diary.service;

import com.plana.diary.dto.request.DiaryCreateRequestDto;

public interface DiaryService {
    void createDiary(DiaryCreateRequestDto request, Long writerId);
}