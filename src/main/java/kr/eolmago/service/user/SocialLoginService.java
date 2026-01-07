package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserProfile;
import kr.eolmago.domain.entity.user.enums.SocialProvider;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.repository.user.SocialLoginRepository;
import kr.eolmago.repository.user.UserProfileRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.notification.publish.NotificationPublishCommand;
import kr.eolmago.service.notification.publish.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SocialLoginService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final NotificationPublisher notificationPublisher;
    private final UserProfileRepository userProfileRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        ParsedOAuthUser parsed = parse(provider, oAuth2User);

        SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());

        final String finalProviderId = parsed.providerId();
        final String finalName = parsed.name();
        final String finalEmail = parsed.email();

        SocialLogin socialLoginUser = socialLoginRepository
            .findByProviderAndProviderId(socialProvider, finalProviderId)
            .orElseGet(() -> createOAuth2User(socialProvider, finalProviderId, finalName, finalEmail));

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("userId", socialLoginUser.getUser().getUserId().toString()); // User PK getter 맞춰
        if (finalEmail != null) {
            attributes.put("email", finalEmail);
        }

        return new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority(socialLoginUser.getUser().getRole().toString())),
            attributes,
            "userId"
        );
    }

    private ParsedOAuthUser parse(String provider, OAuth2User oAuth2User) {
        if ("google".equals(provider)) {
            String providerId = oAuth2User.getAttribute("sub");
            String name = oAuth2User.getAttribute("name");
            String email = oAuth2User.getAttribute("email");
            return new ParsedOAuthUser(providerId, name, email);
        }

        if ("kakao".equals(provider)) {
            Object id = oAuth2User.getAttribute("id");
            String providerId = String.valueOf(id);

            String name = "kakao_" + providerId;
            String email = null;

            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null && profile.get("nickname") != null) {
                    name = (String) profile.get("nickname");
                }
                if (kakaoAccount.get("email") != null) {
                    email = (String) kakaoAccount.get("email");
                }
            }

            return new ParsedOAuthUser(providerId, name, email);
        }

        if ("naver".equals(provider)) {
            Map<String, Object> response = oAuth2User.getAttribute("response");

            if (response != null) {
                String providerId = (String) response.get("id");
                String name = (String) response.get("name");
                String email = (String) response.get("email");
                return new ParsedOAuthUser(providerId, name, email);
            }

            String providerId = oAuth2User.getAttribute("id");
            String name = oAuth2User.getAttribute("name");
            String email = oAuth2User.getAttribute("email");
            return new ParsedOAuthUser(providerId, name, email);
        }

        throw new OAuth2AuthenticationException("지원하지 않는 로그인 제공자");
    }

    private SocialLogin createOAuth2User(SocialProvider provider, String providerId, String name, String email) {
        // ToDo UserRole.GUEST -> 전화번호 인증 -> UserRole.USER
        User newUser = User.create(UserRole.USER);
        User savedUser = userRepository.save(newUser);

        // 2) SocialLogin 생성
        SocialLogin socialLogin = SocialLogin.create(savedUser, provider, providerId, email);
        SocialLogin savedSocialLogin = socialLoginRepository.save(socialLogin);
        log.info("SocialLogin 생성 완료: socialId={}", savedSocialLogin.getSocialId());

        // 3) UserProfile 생성
        String finalName = validateAndProcessName(name, provider, providerId);
        UserProfile userProfile = UserProfile.create(savedUser, finalName, finalName);
        UserProfile savedUserProfile = userProfileRepository.save(userProfile);
        log.info("UserProfile 생성 완료: profileId={}", savedUserProfile.getProfileId());

        // 4) 웰컴 알림 발행
        notificationPublisher.publish(NotificationPublishCommand.welcome(savedUser.getUserId()));

        return savedSocialLogin;
    }

    /**
     * 사용자 이름 검증 및 정제
     */
    private String validateAndProcessName(String name, SocialProvider provider, String providerId) {
        if (name == null || name.trim().isEmpty()) {
            String generatedName = provider.name().toLowerCase() + "_" + providerId.substring(0, Math.min(8, providerId.length()));
            log.warn("빈 이름 처리: provider={}, providerId={}, generated={}",
                    provider, providerId, generatedName);
            return generatedName;
        }
        return name;
    }
    private record ParsedOAuthUser(String providerId, String name, String email) { }
}

