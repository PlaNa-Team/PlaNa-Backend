package com.plana.diary.service;

import com.plana.diary.dto.request.DiaryCreateRequestDto;
import com.plana.diary.dto.response.DiaryCreateResponseDto;
import com.plana.diary.dto.response.DiaryDetailResponseDto;
import com.plana.diary.dto.response.DiaryMonthlyResponseDto;

public interface DiaryService {
    // 다이어리 등록
    DiaryCreateResponseDto createDiary(DiaryCreateRequestDto request, Long writerId);

    // 다이어리 상세 조회
    DiaryDetailResponseDto getDiaryDetail(Long diaryId, Long memberId);

    // 월간 다이어리 조회
    DiaryMonthlyResponseDto getMonthlyDiaries(Long memberId, int year, int month);

}