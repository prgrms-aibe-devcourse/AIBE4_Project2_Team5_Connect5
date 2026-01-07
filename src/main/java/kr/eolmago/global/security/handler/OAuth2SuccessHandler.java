package kr.eolmago.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.SocialProvider;
import kr.eolmago.repository.user.SocialLoginRepository;
import kr.eolmago.service.user.JwtService;
import kr.eolmago.service.user.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final SocialLoginRepository socialLoginRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = authToken.getPrincipal();
            String registrationId = authToken.getAuthorizedClientRegistrationId();

            String providerId = extractProviderId(registrationId, oAuth2User);
            SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());

            SocialLogin socialLogin = socialLoginRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            User user = socialLogin.getUser();

            String accessToken = jwtService.generateAccessToken(user.getUserId(), socialLogin.getEmail(), user.getRole().name());
            String refreshToken = jwtService.generateRefreshToken(user.getUserId());

            refreshTokenService.save(user.getUserId(), refreshToken);

            Cookie accessCookie = new Cookie("accessToken", accessToken);
            accessCookie.setPath("/");
            accessCookie.setHttpOnly(true);
            accessCookie.setMaxAge((int) jwtService.getAccessTokenExpirySeconds());
            response.addCookie(accessCookie);

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setPath("/");
            refreshCookie.setHttpOnly(true);
            refreshCookie.setMaxAge((int) jwtService.getRefreshTokenExpirySeconds());
            response.addCookie(refreshCookie);

            response.sendRedirect("/");
        } catch (Exception e) {
            log.error("OAuth2 Login Success Handler Error", e);
            response.sendRedirect("/login?error");
        }
    }

    private String extractProviderId(String registrationId, OAuth2User oAuth2User) {
        String providerId = null;
        switch (registrationId) {
            case "google":
                providerId = oAuth2User.getAttribute("sub");
                break;
            case "kakao":
                Object idAttribute = oAuth2User.getAttribute("id");
                providerId = String.valueOf(idAttribute);
                break;
            case "naver":
                Map<String, Object> response = oAuth2User.getAttribute("response");
                if (response != null) {
                    providerId = (String) response.get("id");
                } else {
                    providerId = oAuth2User.getAttribute("id");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        }
        return providerId;
    }
}
