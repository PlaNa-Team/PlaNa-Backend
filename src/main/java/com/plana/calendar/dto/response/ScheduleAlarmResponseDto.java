package com.plana.calendar.dto.response;

import com.plana.calendar.enums.NotifyUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일정 알림 정보 응답 DTO
 * 
 * 사용 위치:
 * - ScheduleDetailResponseDto.alarms 필드
 * - ScheduleCreateResponseDto.alarms 필드
 * 
 * 연결 Entity: ScheduleAlarm
 * 사용 Enum: NotifyUnit (MIN, HOUR, DAY)
 * 
 * 주의사항:
 * - 독립적으로 API에서 사용되지 않고, 다른 Response DTO의 중체 객체로만 사용
 * - id 필드는 수정/삭제 시 사용할 수 있도록 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAlarmResponseDto {
    private Long id;
    private Integer notifyBeforeVal;
    private NotifyUnit notifyUnit;
}