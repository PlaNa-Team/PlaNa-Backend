package com.plana.diary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.auth.service.JwtTokenProvider;
import com.plana.diary.dto.request.*;
import com.plana.diary.dto.response.*;
import com.plana.diary.entity.Diary;
import com.plana.diary.service.DiaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    //다이어리 삭제
    @DeleteMapping("/diaries/{diaryId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteDiary(
            @AuthenticationPrincipal AuthenticatedMemberDto authMember,
            @PathVariable Long diaryId){
        // 로그인 사용자 ID
        Long memberId = authMember.getId();

        // 서비스 호출
        diaryService.deleteDiary(diaryId, memberId);

        // 응답생성
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        Map.of("message", "다이어리가 삭제되었습니다.")
                )
        );
    }

    // 다이어리 수정
    @PutMapping("/diaries/{diaryId}")
    public ResponseEntity<DiaryUpdateResponse> updateDiary(
            @PathVariable Long diaryId, //url 경로의 {diaryId} 부분을 매개변수에 매핑
            @AuthenticationPrincipal AuthenticatedMemberDto authMember, //스프링 시큐리티에서 현재 로그인한 사용자의 정보를 주입해주는 어노테이션
            @RequestBody DiaryUpdateRequestDto requestDto //클라이언트가 보낸 JSON 데이터를 DTO 필드에 맞게 변환해준다.
    ) {
        if (authMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2) content가 있을 때만 변환
        if (requestDto.getContent() != null) {
            Object converted = switch (requestDto.getDiaryType()) {
                case DAILY -> objectMapper.convertValue(requestDto.getContent(), DailyContentRequestDto.class);
                case BOOK -> objectMapper.convertValue(requestDto.getContent(), BookContentRequestDto.class);
                case MOVIE -> objectMapper.convertValue(requestDto.getContent(), MovieContentRequestDto.class);
            };
            requestDto.setContent(converted);
        }

        DiaryDetailResponseDto updated = diaryService.updateDiary(diaryId, authMember.getId(), requestDto);

        DiaryUpdateResponse response = DiaryUpdateResponse.builder()
                .status(200)
                .body(DiaryUpdateResponse.Body.builder()
                        .data(updated)
                        .build())
                .build();

        return ResponseEntity.ok(response);

    }
}
