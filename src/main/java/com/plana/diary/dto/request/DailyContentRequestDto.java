package com.plana.diary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyContentRequestDto {
    @NotBlank(message = "제목을 작성해주세요.")
    private String title;

    private String location;
    private String memo;
}
