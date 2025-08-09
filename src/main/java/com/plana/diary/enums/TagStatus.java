package com.plana.diary.enums;

import java.util.Arrays;

public enum TagStatus {
    WRITER("작성자"),
    PENDING("미설정"),
    ACCEPTED("수락"),
    REJECTED("거절"),
    DELETED("삭제");

    private final String displayName;

    TagStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TagStatus fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(status -> status.displayName.equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 태그 상태입니다: " + displayName));
    }
}