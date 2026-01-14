package kr.eolmago.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.eolmago.dto.api.user.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String requestUri = request.getRequestURI();

        // Filter에서 설정한 예외가 있다면 우선 사용
        Exception exception = (Exception) request.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        String message = exception != null ? exception.getMessage() : authException.getMessage();

        // GUEST 사용자가 자격이 필요한 페이지에 접근했을 때
        if (authException instanceof InsufficientAuthenticationException && "GUEST".equals(message)) {
            handleGuestAccess(response);
            return;
        }

        // API 요청인 경우 JSON 응답
        if (requestUri.startsWith("/api/")) {
            // ✨ BANNED 유저 체크 추가
            if (message != null && message.contains("BANNED")) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        ErrorResponse.of(403, "Forbidden", "영구 정지된 이용자입니다.")
                ));
            } else if (message != null && message.contains("SUSPENDED")) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        ErrorResponse.of(403, "Forbidden", "정지된 이용자입니다. 쓰기 작업이 제한됩니다.")
                ));
            } else {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        ErrorResponse.of(401, "Unauthorized", "로그인이 필요합니다.")
                ));
            }
        } else {
            // 웹 페이지 요청인 경우 알림창 띄우고 로그인 페이지로 이동
            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(
                    "<script>alert('로그인이 필요합니다.'); location.href='/login';</script>"
            );
        }
    }

    private void handleGuestAccess(HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write(
                "<script>alert('전화번호 미인증 계정입니다. 전화번호 인증 후 이용 가능합니다.'); location.href='/';</script>"
        );
    }
}
