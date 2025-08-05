package com.plana.file.service;

import com.plana.file.dto.response.TempFileResponseDto;
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
public class FileServiceImpl implements FileService{
    private final String tempDir = "uploads/temp"; //프로젝트 내부 임시 폴더

    @Override
    public TempFileResponseDto saveTempFile(MultipartFile file, Long memberId){
        try{
            // 1. 폴더 생성
            File directory = new File(tempDir);
            if(!directory.exists()){
                directory.mkdirs();
            }

            // 2. 파일명 생성(UUID)
            String tempId = UUID.randomUUID().toString(); // 파일의 기본 이름을 고유하게 만들기 위한 랜덤 ID
            String originalFilename = file.getOriginalFilename(); // 클라이언트가 업로드한 원래 파일 이름을 가져옴
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")); // 원본 파일명에서 확장자만 추출
            String fileName = "temp_" + memberId + "_" + tempId + extension; // 랜덤 UUID + 원래 확장자 조합해서 최종 파일명 생성

            // 3. 실제 파일 저장
            Path filePath = Paths.get(tempDir, fileName); // 지정할 파일의 경로 객체 생성
            Files.write(filePath, file.getBytes()); // 업로드된 파일을 바이트 배열로 변환해서 지정된 경로에 실제 파일을 씀

            // 4. URL & 만료 시간 생성
            String tempUrl = "/uploads/temp/" + fileName; // 정적 매핑 -> 이 파일을 웹에서 접근할 수 있는 주소로 만드는 것
            String expireAt = LocalDateTime.now().plusHours(1)
                    .format(DateTimeFormatter.ISO_DATE_TIME); // 임시 파일이 유효한 시간을 계산

            return new TempFileResponseDto(tempUrl, tempId, expireAt); // 경로, Id, 만료시간을 DTO로 반환
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
