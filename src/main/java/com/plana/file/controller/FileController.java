package com.plana.file.controller;

import com.plana.auth.service.JwtTokenProvider;
import com.plana.file.dto.response.TempFileResponseDto;
import com.plana.file.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/temp-upload")
    public ResponseEntity<?> uploadTempFile(
            HttpServletRequest request,
            @RequestParam("file")MultipartFile file
    ) {
        if (file.isEmpty()){
            return ResponseEntity.badRequest().body("파일이 필요합니다.");
        }

        // jwt 토큰 추출
        String token = jwtTokenProvider.resolveToken(request);
        if(token == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 필요합니다.");
        }

        // 사용자 ID 추출
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        TempFileResponseDto response = fileService.saveTempFile(file, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(201, response) // status + data 구조가 모든 API에서 통일하도록. -> record는 응답의 껍데기 역할을 한다.
        );
    }
    record ApiResponse<T>(int status, T data) {} // T는 실제로 사용할 때 원하는 타입으로 지정 가능한 타입 파라미터다.
}
