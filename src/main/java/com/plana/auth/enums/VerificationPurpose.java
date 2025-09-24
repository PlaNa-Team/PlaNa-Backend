package com.plana.auth.enums;

public enum VerificationPurpose {
    SIGN_UP,        // DB에 없어야 발송
    FIND_ID,        // DB에 있어야 발송
    RESET_PASSWORD; // DB에 있어야 발송

    public boolean shouldExist() {
        return this != SIGN_UP; // SIGN_UP만 "없어야" 함
    }
}
