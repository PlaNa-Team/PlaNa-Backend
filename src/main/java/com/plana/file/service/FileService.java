package com.plana.file.service;

import com.plana.file.dto.response.TempFileResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    // MultipartFile(업로드된 파일)을 받아서 임시 파일로 저장한 뒤
    // 저장 결과를 TempFileResponseDto로 반환
    TempFileResponseDto saveTempFile(MultipartFile file, Long memberId);
}
