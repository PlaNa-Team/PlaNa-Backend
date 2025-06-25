package com.plana.auth.service;

import com.plana.auth.entity.User;
import com.plana.auth.enums.SocialProvider;
import com.plana.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * OAuth2 소셜 로그인 사용자 정보 처리 서비스
 * 구글, 카카오 등에서 받은 사용자 정보를 우리 DB에 저장하거나 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * OAuth2 로그인 성공 시 호출되는 메서드
     * 소셜 서비스에서 받은 사용자 정보를 처리
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 부모 클래스에서 OAuth2User 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // 소셜 서비스 제공업체 확인 (google, kakao 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 Login attempt from: {}", registrationId);
        
        // 사용자 정보 처리 및 DB 저장/업데이트
        User user = processOAuth2User(registrationId, oAuth2User);
        
        // 처리된 사용자 정보를 OAuth2User로 래핑해서 반환
        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    /**
     * OAuth2 사용자 정보 처리 (회원가입 또는 정보 업데이트)
     * @param registrationId 소셜 서비스 제공업체 (google, kakao)
     * @param oAuth2User OAuth2에서 받은 사용자 정보
     * @return 처리된 User 엔티티
     */
    private User processOAuth2User(String registrationId, OAuth2User oAuth2User) {
        // 소셜 제공업체별로 사용자 정보 추출
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // 소셜 제공업체와 제공업체 ID로 기존 사용자 조회
        SocialProvider provider = SocialProvider.fromValue(registrationId);
        User user = userRepository.findByProviderAndProviderId(provider, userInfo.getId())
                .orElse(null);

        if (user != null) {
            // 기존 사용자인 경우: 정보 업데이트
            return updateExistingUser(user, userInfo);
        } else {
            // 새로운 사용자인 경우: 회원가입 처리
            return registerNewUser(provider, userInfo);
        }
    }

    /**
     * 기존 사용자 정보 업데이트
     * @param existingUser 기존 사용자
     * @param userInfo 새로운 사용자 정보
     * @return 업데이트된 사용자
     */
    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        log.info("Updating existing user: {}", existingUser.getEmail());
        
        // 이름이나 프로필 이미지가 변경되었을 수 있으므로 업데이트
        existingUser.setName(userInfo.getName());
        existingUser.setProfileImageUrl(userInfo.getImageUrl());
        
        return userRepository.save(existingUser);
    }

    /**
     * 새로운 사용자 회원가입 처리
     * @param provider 소셜 제공업체
     * @param userInfo 사용자 정보
     * @return 새로 생성된 사용자
     */
    private User registerNewUser(SocialProvider provider, OAuth2UserInfo userInfo) {
        log.info("Registering new user from {}: {}", provider, userInfo.getEmail());
        
        // 같은 이메일로 다른 소셜 계정이 있는지 확인
        if (userRepository.existsByEmail(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException(
                "Email already exists with different social provider: " + userInfo.getEmail()
            );
        }

        // 새 사용자 생성
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .profileImageUrl(userInfo.getImageUrl())
                .provider(provider)
                .providerId(userInfo.getId())
                .role("ROLE_USER") // 기본 권한
                .enabled(true) // 계정 활성화
                .build();

        return userRepository.save(newUser);
    }

    /**
     * 소셜 서비스별 사용자 정보 추출을 위한 팩토리 클래스
     */
    private static class OAuth2UserInfoFactory {
        public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
            switch (registrationId.toLowerCase()) {
                case "google":
                    return new GoogleOAuth2UserInfo(attributes);
                case "kakao":
                    return new KakaoOAuth2UserInfo(attributes);
                default:
                    throw new OAuth2AuthenticationException("Sorry! Login with " + registrationId + " is not supported yet.");
            }
        }
    }

    /**
     * OAuth2 사용자 정보 인터페이스
     */
    private interface OAuth2UserInfo {
        String getId();
        String getName();
        String getEmail();
        String getImageUrl();
    }

    /**
     * 구글 OAuth2 사용자 정보 구현체
     */
    private static class GoogleOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getId() {
            return (String) attributes.get("sub");
        }

        @Override
        public String getName() {
            return (String) attributes.get("name");
        }

        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }

        @Override
        public String getImageUrl() {
            return (String) attributes.get("picture");
        }
    }

    /**
     * 카카오 OAuth2 사용자 정보 구현체
     */
    private static class KakaoOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getId() {
            return String.valueOf(attributes.get("id"));
        }

        @Override
        public String getName() {
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            if (properties == null) {
                return null;
            }
            return (String) properties.get("nickname");
        }

        @Override
        public String getEmail() {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount == null) {
                return null;
            }
            return (String) kakaoAccount.get("email");
        }

        @Override
        public String getImageUrl() {
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            if (properties == null) {
                return null;
            }
            return (String) properties.get("profile_image");
        }
    }

    /**
     * 사용자 정보를 포함한 커스텀 OAuth2User
     */
    public static class CustomOAuth2User implements OAuth2User {
        private final User user;
        private final Map<String, Object> attributes;

        public CustomOAuth2User(User user, Map<String, Object> attributes) {
            this.user = user;
            this.attributes = attributes;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String getName() {
            return user.getName();
        }

        // OAuth2AuthenticatedPrincipal 인터페이스의 필수 메서드
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            // 사용자 권한을 GrantedAuthority 컬렉션으로 반환
            return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
        }

        // 추가: User 엔티티 정보에 쉽게 접근할 수 있도록
        public User getUser() {
            return user;
        }
    }
}
