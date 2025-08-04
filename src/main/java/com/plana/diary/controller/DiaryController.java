package com.plana.diary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plana.auth.service.JwtTokenProvider;
import com.plana.diary.dto.request.BookContentRequestDto;
import com.plana.diary.dto.request.DailyContentRequestDto;
import com.plana.diary.dto.request.DiaryCreateRequestDto;
import com.plana.diary.dto.request.MovieContentRequestDto;
import com.plana.diary.service.DiaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiaryController {
    private final DiaryService diaryService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @PostMapping("/diaries")
    public ResponseEntity<String> createDiary(
            @RequestBody DiaryCreateRequestDto requestDto,
            HttpServletRequest request
            ) {
        // 1. JWT 토큰 추출
        // 사용자가 api를 호출하면 HTTP 요청에 JWT가 담겨 옵니다.
        // resolveToke(HttpServletRequest)는 헤더에서 JWT를 꺼내는 단계이다. "Bearer " 접두어를 제거하고 순수 토큰 문자열만 반환
        String token = jwtTokenProvider.resolveToken(request);
        if(token == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 없습니다. ");
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
        diaryService.createDiary(requestDto, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body("다이어리가 성공적으로 등록되었습니다.");
    }
}
