package kr.eolmago.global.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.SocialProvider;
import kr.eolmago.dto.session.SessionUser;
import kr.eolmago.repository.user.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginRepository socialLoginRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // provider와 providerId 추출
        String provider = extractProviderFromSession(request);
        if (provider == null) {
            provider = extractProvider(request);
        }
        String providerId = extractProviderId(oAuth2User, provider);

        // DB에서 SocialLogin 조회 후 User 정보 추출
        SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());
        SocialLogin socialLogin = socialLoginRepository
                .findByProviderAndProviderId(socialProvider, providerId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        User user = socialLogin.getUser();

        // 세션에 user 정보 저장 (SessionUser DTO 사용)
        HttpSession session = request.getSession();
        SessionUser sessionUser = SessionUser.from(user);
        session.setAttribute("user", sessionUser);

        log.info("로그인 성공: userId={}, provider={}", sessionUser.getUserId(), provider);

        // 기본 성공 URL로 리다이렉트
        setDefaultTargetUrl("/");
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String extractProviderFromSession(HttpServletRequest request) {
        // 세션에서 OAuth2 authorization request 확인
        Object authRequest = request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
        if (authRequest != null) {
            String savedRequest = authRequest.toString();
            if (savedRequest.contains("google")) return "google";
            if (savedRequest.contains("kakao")) return "kakao";
            if (savedRequest.contains("naver")) return "naver";
        }
        return null;
    }

    private String extractProvider(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String referer = request.getHeader("referer");

        // URI나 referer에서 provider 추출
        String combined = requestURI + (referer != null ? referer : "");

        if (combined.contains("google")) {
            return "google";
        } else if (combined.contains("kakao")) {
            return "kakao";
        } else if (combined.contains("naver")) {
            return "naver";
        }

        log.warn("Provider 추출 실패 - URI: {}, Referer: {}", requestURI, referer);
        throw new RuntimeException("알 수 없는 OAuth2 provider");
    }

    private String extractProviderId(OAuth2User oAuth2User, String provider) {
        switch (provider) {
            case "google":
                return oAuth2User.getAttribute("sub");
            case "kakao":
                Object id = oAuth2User.getAttribute("id");
                return String.valueOf(id);
            case "naver":
                Map<String, Object> response = oAuth2User.getAttribute("response");
                if (response != null) {
                    return (String) response.get("id");
                }
                return oAuth2User.getAttribute("id");
            default:
                throw new RuntimeException("지원하지 않는 provider: " + provider);
        }
    }
}
