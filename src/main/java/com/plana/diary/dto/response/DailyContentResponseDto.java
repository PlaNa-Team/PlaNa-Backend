package com.plana.diary.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor //파라미터 없는 기본 생성자를 자동으로 생성
@AllArgsConstructor //모든 필드를 매개변수로 받는 전체 필드 생성자를 자동으로 생성
@Builder //빌더 패턴을 자동으로 생성
public class DailyContentResponseDto implements  DiaryContentResponseDto{
    private String title;
    private String location;
    private String memo;
}
