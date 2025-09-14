package com.plana.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {
    private String url; //클라이언트에서 접근할 수 있는 임시 URL
    private String fileId; //파일명 대신 내부적으로 식별할 ID
    private String expiresAt; //만료시간
}
