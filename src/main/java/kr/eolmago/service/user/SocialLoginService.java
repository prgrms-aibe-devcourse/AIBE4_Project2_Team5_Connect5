package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.SocialProvider;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.repository.user.SocialLoginRepository;
import kr.eolmago.repository.user.UserRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SocialLoginService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String providerId;
        String name;
        String email = null;

        switch (provider) {
            case "google":
                providerId = oAuth2User.getAttribute("sub");
                name = oAuth2User.getAttribute("name");
                email = oAuth2User.getAttribute("email");
                break;
            case "kakao":
                Object idAttribute2 = oAuth2User.getAttribute("id");
                providerId = String.valueOf(idAttribute2);
                Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    if (profile != null) {
                        name = (String) profile.get("nickname");
                    } else {
                        name = "kakao_" + providerId;
                    }
                    if (kakaoAccount.containsKey("email")) {
                        email = (String) kakaoAccount.get("email");
                    }
                } else {
                    name = "kakao_" + providerId;
                }
                break;
            case "naver":
                Map<String, Object> response = oAuth2User.getAttribute("response");
                if (response != null) {
                    providerId = (String) response.get("id");
                    name = (String) response.get("name");
                    email = (String) response.get("email");
                } else {
                    providerId = oAuth2User.getAttribute("id");
                    name = oAuth2User.getAttribute("name");
                    email = oAuth2User.getAttribute("email");
                }
                break;
            default:
                throw new OAuth2AuthenticationException("지원하지 않는 로그인 제공자");
        }

        String finalName = name;
        String finalEmail = email;

        SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());
        SocialLogin socialLoginUser = socialLoginRepository
                .findByProviderAndProviderId(socialProvider, providerId)
                .orElseGet(() -> createOAuth2User(socialProvider, providerId, finalName, finalEmail));

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(socialLoginUser.getUser().getRole().toString())),
                oAuth2User.getAttributes(),
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
        );
    }

    private SocialLogin createOAuth2User(SocialProvider provider, String providerId, String name, String email) {
        // 1. User 엔티티 생성
        User newUser = User.create(UserRole.USER);
        User savedUser = userRepository.save(newUser);

        // 2. SocialLogin 엔티티 생성
        SocialLogin socialLogin = SocialLogin.create(
                savedUser,
                provider,
                providerId,
                email
        );

        return socialLoginRepository.save(socialLogin);
    }
}
