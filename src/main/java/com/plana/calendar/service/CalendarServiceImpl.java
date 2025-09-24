package com.plana.calendar.service;

import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.calendar.dto.request.ScheduleCreateRequestDto;
import com.plana.calendar.dto.request.ScheduleUpdateRequestDto;
import com.plana.calendar.dto.response.*;
import com.plana.calendar.entity.Category;
import com.plana.calendar.entity.Schedule;
import com.plana.calendar.entity.ScheduleAlarm;
import com.plana.calendar.repository.CategoryRepository;
import com.plana.calendar.repository.ScheduleRepository;
import com.plana.calendar.repository.ScheduleAlarmRepository;
import com.plana.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // 기본값을 readOnly = true 로 설정. 메서드에서 오버라이드 (쓰기 설정해서 사용: @Transactional)
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    // @RequiredArgsConstructor 로 생성자 주입
    private final ScheduleRepository scheduleRepository;
    private final CategoryRepository categoryRepository;
    private final ScheduleAlarmRepository scheduleAlarmRepository;
    private final MemberRepository memberRepository;
    private final RecurrenceService recurrenceService;
    private final NotificationService notificationService;

    @Override
    public List<ScheduleMonthlyItemDto> getMonthlySchedules(Long memberId, int year, int month) {
        // 월 범위 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<ScheduleMonthlyItemDto> result = new ArrayList<>();

        // 1. 일반 일정 조회 및 DTO 변환
        List<Schedule> nonRecurringSchedules = scheduleRepository.findNonRecurringSchedulesInRange(
                memberId, startOfMonth, endOfMonth);
        
        for (Schedule schedule : nonRecurringSchedules) {
            result.add(convertToMonthlyItemDto(schedule, null)); // virtualId = null
        }

        // 2. 반복 일정 조회 및 인스턴스 생성
        List<Schedule> recurringSchedules = scheduleRepository.findRecurringSchedulesForRange(
                memberId, endOfMonth);

        for (Schedule recurringSchedule : recurringSchedules) {
            // 해당 월에 포함되는 반복 인스턴스들 생성
            List<LocalDateTime> instances = recurrenceService.generateInstancesInRange(
                    recurringSchedule.getRecurrenceRule(),
                    recurringSchedule.getStartAt(),
                    startOfMonth,
                    endOfMonth
            );

            for (LocalDateTime instanceStart : instances) {
                // virtualId 생성: "recurring-{scheduleId}-{timestamp}"
                String virtualId = String.format("recurring-%d-%d", 
                    recurringSchedule.getId(), 
                    instanceStart.toEpochSecond(java.time.ZoneOffset.UTC));
                
                result.add(convertToMonthlyItemDto(recurringSchedule, virtualId, instanceStart));
            }
        }

        // 3. 시간 순으로 정렬
        return result.stream()
                .sorted(Comparator.comparing(ScheduleMonthlyItemDto::getStartAt))
                .collect(Collectors.toList());
    }

    @Override
    public ScheduleDetailResponseDto getScheduleDetail(Long scheduleId, Long memberId) {
        Schedule schedule = scheduleRepository.findByIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        return convertToDetailResponseDto(schedule);
    }

    @Override
    @Transactional
    public ScheduleDetailResponseDto createSchedule(ScheduleCreateRequestDto createDto, Long memberId) {
        // Member 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // Category 조회 (categoryId가 있을 때만)
        Category category = null;
        if (createDto.getCategoryId() != null) {
            category = categoryRepository.findByIdAndMemberId(createDto.getCategoryId(), memberId)
                    .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
        }

        // Schedule 생성
        Schedule schedule = Schedule.builder()
                .member(member)
                .category(category)
                .title(createDto.getTitle())
                .description(createDto.getDescription())
                .startAt(createDto.getStartAt())
                .endAt(createDto.getEndAt())
                .isAllDay(createDto.getIsAllDay())
                .color(createDto.getColor())
                .isRecurring(createDto.getRecurrenceRule() != null && !createDto.getRecurrenceRule().trim().isEmpty())
                .recurrenceRule(createDto.getRecurrenceRule())
                .recurrenceUntil(createDto.getRecurrenceUntil())
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);

        // 알림 생성
        if (createDto.getAlarms() != null && !createDto.getAlarms().isEmpty()) {
            for (var alarmDto : createDto.getAlarms()) {
                ScheduleAlarm alarm = ScheduleAlarm.builder()
                        .schedule(savedSchedule)
                        .notifyBeforeVal(alarmDto.getNotifyBeforeVal())
                        .notifyUnit(alarmDto.getNotifyUnit())
                        .build();
                ScheduleAlarm savedAlarm = scheduleAlarmRepository.save(alarm);

                // Notification 생성 (스케줄 알람용)
                try {
                    String message = String.format("%d%s 후 '%s'가 시작됩니다",
                            alarmDto.getNotifyBeforeVal(),
                            getUnitDisplayName(alarmDto.getNotifyUnit().name()),
                            savedSchedule.getTitle());

                    notificationService.createScheduleNotification(savedAlarm.getId(), memberId, message);
                } catch (Exception e) {
                    // 알림 생성 실패 시 로깅만 하고 스케줄 생성은 계속 진행
                    System.err.println("스케줄 알림 생성 실패: " + e.getMessage());
                }
            }
        }

        return convertToDetailResponseDto(savedSchedule);
    }

    @Override
    @Transactional
    public ScheduleDetailResponseDto updateSchedule(Long scheduleId, ScheduleUpdateRequestDto updateDto, Long memberId) {
        Schedule schedule = scheduleRepository.findByIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        // 부분 업데이트 (PATCH 방식)
        if (updateDto.getTitle() != null) {
            schedule.setTitle(updateDto.getTitle());
        }
        if (updateDto.getDescription() != null) {
            schedule.setDescription(updateDto.getDescription());
        }
        if (updateDto.getStartAt() != null) {
            schedule.setStartAt(updateDto.getStartAt());
        }
        if (updateDto.getEndAt() != null) {
            schedule.setEndAt(updateDto.getEndAt());
        }
        if (updateDto.getIsAllDay() != null) {
            schedule.setIsAllDay(updateDto.getIsAllDay());
        }
        if (updateDto.getColor() != null) {
            schedule.setColor(updateDto.getColor());
        }
        if (updateDto.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndMemberId(updateDto.getCategoryId(), memberId)
                    .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
            schedule.setCategory(category);
        }
        if (updateDto.getRecurrenceRule() != null) {
            schedule.setRecurrenceRule(updateDto.getRecurrenceRule());
            schedule.setIsRecurring(!updateDto.getRecurrenceRule().trim().isEmpty());
        }
        if (updateDto.getRecurrenceUntil() != null) {
            schedule.setRecurrenceUntil(updateDto.getRecurrenceUntil());
        }

        // 알림 업데이트 (기존 알림 삭제 후 새로 생성)
        if (updateDto.getAlarms() != null) {
            scheduleAlarmRepository.deleteByScheduleId(scheduleId);
            
            for (var alarmDto : updateDto.getAlarms()) {
                ScheduleAlarm alarm = ScheduleAlarm.builder()
                        .schedule(schedule)
                        .notifyBeforeVal(alarmDto.getNotifyBeforeVal())
                        .notifyUnit(alarmDto.getNotifyUnit())
                        .build();
                scheduleAlarmRepository.save(alarm);
            }
        }

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        return convertToDetailResponseDto(updatedSchedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId, Long memberId) {
        Schedule schedule = scheduleRepository.findByIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        schedule.setIsDeleted(true);
        scheduleRepository.save(schedule);
    }

    /**
     * 일반 일정용 DTO 변환 (virtualId = null)
     */
    private ScheduleMonthlyItemDto convertToMonthlyItemDto(Schedule schedule, String virtualId) {
        return new ScheduleMonthlyItemDto(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getIsAllDay(),
                schedule.getColor(),
                schedule.getIsRecurring(),
                schedule.getCategory() != null ? schedule.getCategory().getName() : null,
                virtualId
        );
    }

    /**
     * 반복 일정 인스턴스용 DTO 변환 (시간 재계산)
     */
    private ScheduleMonthlyItemDto convertToMonthlyItemDto(Schedule schedule, String virtualId, LocalDateTime instanceStart) {
        // 인스턴스의 종료 시간 계산
        LocalDateTime instanceEnd = null;
        if (schedule.getEndAt() != null) {
            long duration = java.time.Duration.between(schedule.getStartAt(), schedule.getEndAt()).getSeconds();
            instanceEnd = instanceStart.plusSeconds(duration);
        }

        return new ScheduleMonthlyItemDto(
                schedule.getId(), // 원본 ID 유지
                schedule.getTitle(),
                instanceStart,     // 계산된 시작 시간
                instanceEnd,       // 계산된 종료 시간
                schedule.getIsAllDay(),
                schedule.getColor(),
                schedule.getIsRecurring(),
                schedule.getCategory() != null ? schedule.getCategory().getName() : null,
                virtualId          // "recurring-123-1707134400"
        );
    }

    /**
     * 상세 조회용 DTO 변환
     */
    private ScheduleDetailResponseDto convertToDetailResponseDto(Schedule schedule) {
        // 카테고리 정보 (category가 null일 수 있음)
        CategoryResponseDto categoryDto = null;
        if (schedule.getCategory() != null) {
            categoryDto = new CategoryResponseDto(
                    schedule.getCategory().getId(),
                    schedule.getCategory() != null ? schedule.getCategory().getName() : null,
                    schedule.getCategory().getColor()
            );
        }

        // 알림 정보
        List<ScheduleAlarmResponseDto> alarmDtos = scheduleAlarmRepository.findByScheduleId(schedule.getId());

        return new ScheduleDetailResponseDto(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getIsAllDay(),
                schedule.getColor(),
                schedule.getIsRecurring(),
                schedule.getRecurrenceRule(),
                schedule.getRecurrenceUntil(),
                !alarmDtos.isEmpty(), // hasNotification
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                categoryDto,
                alarmDtos
        );
    }

    /**
     *
     * @param memberId
     * @param keyword
     * @return
     */
    public List<ScheduleSearchResponseDto> search(Long memberId, String keyword) {
        return scheduleRepository.searchByKeyword(memberId, keyword)
                .stream()
                .map(ScheduleSearchResponseDto::from)
                .toList();
    }

    /**
     * 알림 단위 표시명 변환
     */
    private String getUnitDisplayName(String unit) {
        return switch (unit) {
            case "MIN" -> "분";
            case "HOUR" -> "시간";
            case "DAY" -> "일";
            default -> unit;
        };
    }
}