package com.plana.calendar.enums;

/**
 * NotifyUnit 은 calendar의 alarm entity 에서 사용하는 enum 타입임.
 *
 * 알림 시간 단위를 정의하는 Enum
 * 일정 알림에서 notifyBeforeVal 과 함께 사용하여 알림 시간을 결정
 * 
 * 예시:
 * - notifyBeforeVal: 30, notifyUnit: MIN -> 일정 30분 전 알림
 * - notifyBeforeVal: 2, notifyUnit: HOUR -> 일정 2시간 전 알림
 * - notifyBeforeVal: 1, notifyUnit: DAY -> 일정 1일 전 알림
 * 
 * recurrenceRule과는 별개의 개념입니다:
 * - NotifyUnit: 알림 시간 단위 (분/시간/일)
 * - recurrenceRule: 일정 반복 규칙 (RFC 5545 RRule 형식)
 */
public enum NotifyUnit {
    MIN,    // 분
    HOUR,   // 시간
    DAY     // 일
}