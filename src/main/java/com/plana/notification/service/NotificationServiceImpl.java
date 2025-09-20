package com.plana.notification.service;

import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.calendar.entity.ScheduleAlarm;
import com.plana.calendar.repository.ScheduleAlarmRepository;
import com.plana.diary.entity.DiaryTag;
import com.plana.diary.repository.DiaryTagRepository;
import com.plana.notification.dto.response.NotificationListResponseDto;
import com.plana.notification.dto.response.NotificationResponseDto;
import com.plana.notification.entity.Notification;
import com.plana.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final DiaryTagRepository diaryTagRepository;
    private final ScheduleAlarmRepository scheduleAlarmRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponseDto getNotifications(Long memberId, Pageable pageable, boolean unreadOnly) {
        Page<Notification> notificationPage;

        if (unreadOnly) {
            notificationPage = notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId, pageable);
        } else {
            notificationPage = notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        }

        List<NotificationResponseDto> notifications = notificationPage.getContent().stream()
                .map(this::convertToResponseDto)
                .toList();

        long unreadCount = notificationRepository.countByMemberIdAndIsReadFalse(memberId);

        NotificationListResponseDto.Pagination pagination = NotificationListResponseDto.Pagination.builder()
                .currentPage(notificationPage.getNumber())
                .totalPages(notificationPage.getTotalPages())
                .totalCount(notificationPage.getTotalElements())
                .unreadCount(unreadCount)
                .size(notificationPage.getSize())
                .build();

        return NotificationListResponseDto.builder()
                .data(notifications)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 알림에 대한 권한이 없습니다.");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return convertToResponseDto(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long memberId) {
        return notificationRepository.markAllAsReadByMemberId(memberId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public NotificationResponseDto createDiaryTagNotification(Long diaryTagId, Long targetMemberId, String message) {
        DiaryTag diaryTag = diaryTagRepository.findById(diaryTagId)
                .orElseThrow(() -> new IllegalArgumentException("다이어리 태그를 찾을 수 없습니다."));

        Member targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 중복 알림 방지
        if (notificationRepository.existsByDiaryTagId(diaryTagId)) {
            throw new IllegalArgumentException("이미 해당 다이어리 태그에 대한 알림이 존재합니다.");
        }

        Notification notification = Notification.builder()
                .diaryTag(diaryTag)
                .member(targetMember)
                .type("TAG")
                .time(LocalDateTime.now())
                .isRead(false)
                .isSent(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // 실시간 알림 발송
        sendRealTimeNotification(savedNotification);

        return convertToResponseDto(savedNotification);
    }

    @Override
    @Transactional
    public NotificationResponseDto createScheduleNotification(Long scheduleAlarmId, Long targetMemberId, String message) {
        ScheduleAlarm scheduleAlarm = scheduleAlarmRepository.findById(scheduleAlarmId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄 알람을 찾을 수 없습니다."));

        Member targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 알림 시각 계산
        LocalDateTime notifyTime = calculateNotifyTime(scheduleAlarm);

        Notification notification = Notification.builder()
                .scheduleAlarm(scheduleAlarm)
                .member(targetMember)
                .type("ALARM")
                .time(notifyTime)
                .isRead(false)
                .isSent(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // 현재 시간이면 즉시 발송, 미래 시간이면 스케줄러가 처리
        if (notifyTime.isBefore(LocalDateTime.now()) || notifyTime.isEqual(LocalDateTime.now())) {
            sendRealTimeNotification(savedNotification);
        }

        return convertToResponseDto(savedNotification);
    }

    @Override
    public void sendRealTimeNotification(Notification notification) {
        Long memberId = notification.getMember().getId();

        try {
            // 사용자가 온라인인지 확인
            boolean isOnline = sessionManager.isUserOnline(memberId);

            if (isOnline) {
                // 온라인 사용자에게 실시간 알림 발송
                String destination = "/user/" + memberId + "/notifications";
                NotificationResponseDto responseDto = convertToResponseDto(notification);

                messagingTemplate.convertAndSend(destination, responseDto);
                log.info("실시간 알림 발송 완료: memberId={}, notificationId={}, sessionCount={}",
                        memberId, notification.getId(), sessionManager.getUserSessionCount(memberId));
            } else {
                // 오프라인 사용자는 발송 안함 (다음 로그인 시 REST API로 조회)
                log.debug("오프라인 사용자 알림 저장: memberId={}, notificationId={}",
                        memberId, notification.getId());
            }

            // 온라인/오프라인 상관없이 발송 완료 처리 (DB에 저장된 것으로 간주)
            notification.setIsSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("실시간 알림 발송 실패: memberId={}, notificationId={}, error={}",
                    memberId, notification.getId(), e.getMessage(), e);
            throw e; // 발송 실패 시 예외 재발생으로 isSent 상태 유지
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByMemberIdAndIsReadFalse(memberId);
    }

    /**
     * Notification 엔티티를 ResponseDto로 변환
     */
    private NotificationResponseDto convertToResponseDto(Notification notification) {
        Map<String, Object> relatedData = new HashMap<>();
        String message = "";

        if ("TAG".equals(notification.getType()) && notification.getDiaryTag() != null) {
            DiaryTag diaryTag = notification.getDiaryTag();
            relatedData.put("diaryId", diaryTag.getDiary().getId());
            relatedData.put("diaryDate", diaryTag.getDiary().getDiaryDate());
            relatedData.put("diaryType", diaryTag.getDiary().getType());

            if (diaryTag.getDiary().getWriter() != null) {
                relatedData.put("writerName", diaryTag.getDiary().getWriter().getName());
                relatedData.put("writerId", diaryTag.getDiary().getWriter().getId());
                message = String.format("%s님이 다이어리에 회원님을 태그했습니다",
                        diaryTag.getDiary().getWriter().getName());
            }

        } else if ("ALARM".equals(notification.getType()) && notification.getScheduleAlarm() != null) {
            ScheduleAlarm scheduleAlarm = notification.getScheduleAlarm();
            relatedData.put("scheduleId", scheduleAlarm.getSchedule().getId());
            relatedData.put("scheduleTitle", scheduleAlarm.getSchedule().getTitle());
            relatedData.put("startAt", scheduleAlarm.getSchedule().getStartAt());

            message = String.format("%d%s 후 '%s'가 시작됩니다",
                    scheduleAlarm.getNotifyBeforeVal(),
                    getUnitDisplayName(scheduleAlarm.getNotifyUnit().name()),
                    scheduleAlarm.getSchedule().getTitle());
        }

        return NotificationResponseDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(message)
                .time(notification.getTime())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .relatedData(relatedData)
                .build();
    }

    /**
     * 스케줄 알람의 알림 시각 계산
     */
    private LocalDateTime calculateNotifyTime(ScheduleAlarm scheduleAlarm) {
        LocalDateTime scheduleStartTime = scheduleAlarm.getSchedule().getStartAt();
        int notifyBefore = scheduleAlarm.getNotifyBeforeVal();

        return switch (scheduleAlarm.getNotifyUnit()) {
            case MIN -> scheduleStartTime.minusMinutes(notifyBefore);
            case HOUR -> scheduleStartTime.minusHours(notifyBefore);
            case DAY -> scheduleStartTime.minusDays(notifyBefore);
        };
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