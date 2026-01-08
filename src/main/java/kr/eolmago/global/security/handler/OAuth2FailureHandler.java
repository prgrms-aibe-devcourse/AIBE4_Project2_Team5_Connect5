package kr.eolmago.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);

        String errorCode = "UNKNOWN_ERROR";
        String errorMessage = "로그인에 실패했습니다.";

        if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException) {
            org.springframework.security.oauth2.core.OAuth2AuthenticationException oauth2Ex =
                    (org.springframework.security.oauth2.core.OAuth2AuthenticationException) exception;

            org.springframework.security.oauth2.core.OAuth2Error error = oauth2Ex.getError();

            log.debug("OAuth2AuthenticationException 감지, error={}", error);

            if (error != null) {
                String errorCodeFromError = error.getErrorCode();
                log.debug("OAuth2Error 추출 - errorCode: {}", errorCodeFromError);

                if ("U004".equals(errorCodeFromError)) {
                    log.info("✅ BANNED 유저 감지됨!");
                    errorCode = "U004";
                    errorMessage = "영구 정지된 이용자입니다.";
                }
            } else {
                log.debug("❌ OAuth2Error가 null입니다");
            }
        }

        // 로그인 페이지로 리다이렉트하면서 에러 코드 전달
        String redirectUrl = "/login?error=true&errorCode=" + errorCode + "&errorMessage=" +
                java.net.URLEncoder.encode(errorMessage, java.nio.charset.StandardCharsets.UTF_8);

        log.info("OAuth2 로그인 실패 리다이렉트: errorCode={}, url={}", errorCode, redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
