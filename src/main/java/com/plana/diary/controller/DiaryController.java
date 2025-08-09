package com.plana.diary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plana.auth.service.JwtTokenProvider;
import com.plana.diary.dto.request.BookContentRequestDto;
import com.plana.diary.dto.request.DailyContentRequestDto;
import com.plana.diary.dto.request.DiaryCreateRequestDto;
import com.plana.diary.dto.request.MovieContentRequestDto;
import com.plana.diary.dto.response.ApiResponse;
import com.plana.diary.dto.response.DiaryCreateResponseDto;
import com.plana.diary.dto.response.DiaryDetailResponseDto;
import com.plana.diary.dto.response.DiaryMonthlyResponseDto;
import com.plana.diary.service.DiaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiaryController {
    private final DiaryService diaryService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;


    // 다이어리 저장
    @PostMapping("/diaries")
    public ResponseEntity<ApiResponse<DiaryCreateResponseDto>> createDiary(
            @RequestBody DiaryCreateRequestDto requestDto,
            HttpServletRequest request
            ) {
        // 1. JWT 토큰 추출
        // 사용자가 api를 호출하면 HTTP 요청에 JWT가 담겨 옵니다.
        // resolveToke(HttpServletRequest)는 헤더에서 JWT를 꺼내는 단계이다. "Bearer " 접두어를 제거하고 순수 토큰 문자열만 반환
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "토큰이 없습니다."));
        }

        // 2. 토큰에서 사용자 ID 추출
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        // 3. content를 diaryType에 따라 변환
        Object convertedContent = switch(requestDto.getDiaryType()){
            case DAILY -> objectMapper.convertValue(requestDto.getContent(), DailyContentRequestDto.class);
            case MOVIE -> objectMapper.convertValue(requestDto.getContent(), MovieContentRequestDto.class);
            case BOOK -> objectMapper.convertValue(requestDto.getContent(), BookContentRequestDto.class);
        };
        requestDto.setContent(convertedContent);

        // 3. 서비스 호출
        DiaryCreateResponseDto responseDto = diaryService.createDiary(requestDto, memberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), responseDto));
    }

    // 다이어리 조회
    @GetMapping("/diaries/{diaryId}")
    public ResponseEntity<DiaryDetailResponseDto> getDiaryDetail(
            @PathVariable Long diaryId, //URL 경로에서 diaryId 값을 변수로 받는다.
            HttpServletRequest request
    ){
        // 1. JWT 토큰 확인
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        // 2. 서비스 호출
        DiaryDetailResponseDto responseDto = diaryService.getDiaryDetail(diaryId, memberId);

        return ResponseEntity.ok(responseDto);
    }


    //월간 다이어리 조회
    @GetMapping("/diaries")
    public ResponseEntity<ApiResponse<DiaryMonthlyResponseDto>> getMonthlyDiaries(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request
    ){
        // JWT 토큰 확인
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        // 서비스 호출
        DiaryMonthlyResponseDto data = diaryService.getMonthlyDiaries(memberId, year, month);

        // 응답
        String msg = String.format("%d월 월간 다이어리 데이터 조회 성공", month);
        ApiResponse<DiaryMonthlyResponseDto> res =
                ApiResponse.<DiaryMonthlyResponseDto>builder()
                        .status(200)
                        .message(msg)
                        .body(new ApiResponse.Body<>(data))  // data를 body 안에 넣기
                        .build();

        return ResponseEntity.ok(res);
    }
}
