package com.plana.calendar.dto.request;

import com.plana.calendar.enums.NotifyUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일정 알림 설정 요청 DTO
 * 
 * 사용 API: POST /api/calendars, PUT /api/calendars/{id}
 * 사용 위치: ScheduleCreateRequestDto.alarms, ScheduleUpdateRequestDto.alarms 내부
 * 
 * 연결 Entity: ScheduleAlarm
 * 사용 Enum: NotifyUnit (MIN, HOUR, DAY)
 * 
 * 예시:
 * - notifyBeforeVal: 30, notifyUnit: MIN -> 30분 전 알림
 * - notifyBeforeVal: 1, notifyUnit: HOUR -> 1시간 전 알림
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAlarmRequestDto {
    private Integer notifyBeforeVal;
    private NotifyUnit notifyUnit;
}