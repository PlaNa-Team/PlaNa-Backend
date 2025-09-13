package com.plana.file.service;

import com.plana.file.dto.response.FileUploadResponseDto; // ★ 이름 바꿀 경우
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final String uploadDir = "uploads/diary";

    @Override
    public FileUploadResponseDto saveImageFile(MultipartFile file, Long memberId) {
        try {
            // 1. 폴더 생성
            File directory = new File(uploadDir);
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("업로드 폴더 생성 실패: " + uploadDir);
            }

            // 2. 확장자 안전 추출
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            // 3. 파일명 생성: diary_회원ID_타임스탬프_랜덤8자리
            String randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "diary_" + memberId + "_" + timestamp + "_" + randomId + extension;

            // 4. 실제 저장
            Path filePath = Paths.get(uploadDir, fileName);
            Files.write(filePath, file.getBytes());

            // 5. URL & 만료 시간
            String publicUrl = "/uploads/diary/" + fileName;
            String expireAt = LocalDateTime.now().plusHours(1)
                    .format(DateTimeFormatter.ISO_DATE_TIME);

            return new FileUploadResponseDto(publicUrl, randomId, expireAt);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
        }
    }
}
