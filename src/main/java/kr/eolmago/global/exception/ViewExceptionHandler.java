package kr.eolmago.global.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
@ControllerAdvice("kr.eolmago.controller.view")
public class ViewExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public void handleBusinessException(BusinessException e, HttpServletResponse response) throws IOException {
        log.warn("[ViewExceptionHandler] BusinessException: {}", e.getMessage());

        ErrorCode errorCode = e.getErrorCode();
        String message;
        String script;

        if (errorCode.getStatus() == HttpStatus.FORBIDDEN) {
            message = "권한이 없습니다.";
            script = "history.back();";
        } else if (errorCode.getStatus() == HttpStatus.UNAUTHORIZED) {
            message = "로그인이 필요합니다.";
            script = "location.href='/login';";
        } else {
            message = e.getMessage();
            script = "history.back();"; // 그 외 다른 에러는 이전 페이지로 이동
        }

        response.setContentType("text/html; charset=UTF-8");
        // 브라우저가 스크립트를 정상적으로 실행하도록 상태 코드를 200 OK로 설정합니다.
        response.setStatus(HttpStatus.OK.value());
        PrintWriter out = response.getWriter();
        out.println("<script>alert('" + message + "'); " + script + "</script>");
        out.flush();
    }
}
