package com.plana.diary.dto.request;

import com.plana.diary.enums.DiaryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DiaryCreateRequestDto {
    @NotNull private LocalDate diaryDate;
    @NotNull private DiaryType diaryType;
    private String imageUrl;
    private Object content; // diaryType에 맞게 내용이 바껴야한다. 그래서 모든 타입의 최상위 클래스 object를 사용한다.
    private List<DiaryTagRequestDto> diaryTags;
}
