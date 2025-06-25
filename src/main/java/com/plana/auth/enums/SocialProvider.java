package com.plana.auth.enums;

public enum SocialProvider {
    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver"),
    LOCAL("local"); // 일반 회원가입용

    private final String value;

    SocialProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SocialProvider fromValue(String value) {
        for (SocialProvider provider : SocialProvider.values()) {
            if (provider.getValue().equals(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + value);
    }
}
