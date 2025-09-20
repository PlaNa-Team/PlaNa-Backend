package com.plana.notification.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.notification.dto.response.ApiResponse;
import com.plana.notification.dto.response.NotificationListResponseDto;
import com.plana.notification.dto.response.NotificationResponseDto;
import com.plana.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 관련 REST API 및 WebSocket 메시징 컨트롤러
 *
 * 구현 기능:
 * - 알림 목록 조회 (페이징, 필터링)
 * - 개별 알림 읽음 처리
 * - 전체 알림 읽음 처리
 * - WebSocket 클라이언트 연결 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회 API
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param unreadOnly 안읽은 알림만 조회 여부 (기본값: false)
     * @param authMember 인증된 사용자 정보
     * @return 알림 목록과 페이징 정보
     */
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponseDto>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {

        log.info("GET /api/notifications - memberId: {}, page: {}, size: {}, unreadOnly: {}",
                authMember.getId(), page, size, unreadOnly);

        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "인증이 필요합니다."));
            }

            Pageable pageable = PageRequest.of(page, size);
            NotificationListResponseDto notifications = notificationService.getNotifications(
                    authMember.getId(), pageable, unreadOnly);

            String message = unreadOnly ? "안읽은 알림 조회 성공" : "알림 목록 조회 성공";
            return ResponseEntity.ok(ApiResponse.success(message, notifications));

        } catch (Exception e) {
            log.error("알림 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "알림 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 개별 알림 읽음 처리 API
     *
     * @param notificationId 읽음 처리할 알림 ID
     * @param authMember 인증된 사용자 정보
     * @return 읽음 처리 결과
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {

        log.info("PUT /api/notifications/{}/read - memberId: {}", notificationId, authMember.getId());

        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "인증이 필요합니다."));
            }

            NotificationResponseDto updatedNotification = notificationService.markAsRead(
                    notificationId, authMember.getId());

            return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다.", updatedNotification));

        } catch (IllegalArgumentException e) {
            log.warn("알림 읽음 처리 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("알림 읽음 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "알림 읽음 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 전체 알림 읽음 처리 API
     *
     * @param authMember 인증된 사용자 정보
     * @return 읽음 처리된 알림 개수
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {

        log.info("PUT /api/notifications/read-all - memberId: {}", authMember.getId());

        try {
            if (authMember == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "인증이 필요합니다."));
            }

            int updatedCount = notificationService.markAllAsRead(authMember.getId());

            String message = String.format("%d개의 알림을 읽음 처리했습니다.", updatedCount);
            return ResponseEntity.ok(ApiResponse.success(message, updatedCount));

        } catch (Exception e) {
            log.error("전체 알림 읽음 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "전체 알림 읽음 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * WebSocket 클라이언트 연결 처리
     * 클라이언트가 WebSocket에 연결되면 호출되는 메시지 핸들러
     *
     * @param headerAccessor WebSocket 메시지 헤더 정보
     */
    @MessageMapping("/connect")
    public void handleConnect(SimpMessageHeaderAccessor headerAccessor) {
        try {
            // JWT 토큰에서 사용자 ID 추출 (향후 구현)
            String sessionId = headerAccessor.getSessionId();
            log.info("WebSocket 클라이언트 연결: sessionId = {}", sessionId);

            // 향후 사용자별 세션 관리 로직 추가 예정
            // String memberId = extractMemberIdFromToken(headerAccessor);
            // notificationService.addUserSession(memberId, sessionId);

        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 해제 처리
     * (향후 @EventListener를 통해 구현 예정)
     */
    // @EventListener
    // public void handleDisconnect(SessionDisconnectEvent event) { ... }
}