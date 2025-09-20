package com.plana.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 알림 목록 응답 DTO
 * 페이징된 알림 목록과 관련 메타데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDto {

    private List<NotificationResponseDto> data;

    private Pagination pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private int currentPage;
        private int totalPages;
        private long totalCount;
        private long unreadCount;
        private int size;
    }
}