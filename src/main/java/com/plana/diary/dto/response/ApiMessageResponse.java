package com.plana.diary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiMessageResponse {
    private int status;
    private String message;

    public static ApiMessageResponse of(int status, String msg) {
        return new ApiMessageResponse(status, msg);
    }
}
