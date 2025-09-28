package com.plana.diary.service;

import com.plana.diary.dto.request.DiaryCreateRequestDto;
import com.plana.diary.dto.request.DiaryUpdateRequestDto;
import com.plana.diary.dto.response.DiaryCreateResponseDto;
import com.plana.diary.dto.response.DiaryDetailResponseDto;
import com.plana.diary.dto.response.DiaryMonthlyResponseDto;
import com.plana.diary.dto.response.TagStatusUpdateResponseDto;

import java.time.LocalDate;

public interface DiaryService {
    // 다이어리 등록
    DiaryCreateResponseDto createDiary(DiaryCreateRequestDto request, Long writerId);

    // 다이어리 상세 조회
    DiaryDetailResponseDto getDiaryDetailByDate(LocalDate date, Long memberId);

    // 월간 다이어리 조회
    DiaryMonthlyResponseDto getMonthlyDiaries(Long memberId, int year, int month);

    // 다이어리 삭제
    void deleteDiary(Long diaryId, Long memberId);

    // 다이어리 수정
    DiaryDetailResponseDto updateDiary(Long diaryId, Long memberId, DiaryUpdateRequestDto requestDto, String lockToken);

    TagStatusUpdateResponseDto updateDiaryTagStatus(Long tagId, Long memberId, String tagStatus);

}