package com.plana.calendar.dto.response;

import com.plana.calendar.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleSearchResponseDto {
    private Long id;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String color;

    public static ScheduleSearchResponseDto from(Schedule s) {
        return new ScheduleSearchResponseDto(
                s.getId(),
                s.getTitle(),
                s.getStartAt(),
                s.getEndAt(),
                s.getColor()
        );
    }
}
