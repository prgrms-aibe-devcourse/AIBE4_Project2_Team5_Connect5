package kr.eolmago.global.exception;


import jakarta.servlet.http.HttpServletRequest;
import kr.eolmago.service.notification.exception.NotificationErrorCode;
import kr.eolmago.service.notification.exception.NotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "kr.eolmago.controller.api")
public class GlobalExceptionHandler {

    /**
     * 정적 리소스 없음 (Spring MVC 6)
     * - 보통은 여기(basePackages 제한)에서 잡히지 않을 수 있으나,
     *   요청하신 대로 핸들러는 추가합니다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        // 정적 리소스 404는 서버 장애가 아니므로 과도한 로그를 남기지 않는 편이 낫습니다.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * 비즈니스 예외 처리 (AuctionException)
     */
    @ExceptionHandler(AuctionException.class)
    public ResponseEntity<ErrorResponse> handleAuctionException(
            AuctionException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "AuctionException");

        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode, e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * Notification 비즈니스 예외
     */
    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotificationException(
            NotificationException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "NotificationException");

        NotificationErrorCode ec = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(ec.getCode(), e.getMessage());

        return ResponseEntity
                .status(ec.getStatus())
                .body(response);
    }

    /**
     * {@code @Valid} 검증 실패 ({@code @RequestBody})
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "MethodArgumentNotValidException");

        List<ErrorResponse.FieldError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.of(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * {@code @ModelAttribute} 검증 실패
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "BindException");

        List<ErrorResponse.FieldError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.of(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * {@code @PathVariable}, {@code @RequestParam} 검증 실패
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "HandlerMethodValidationException");

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE,
                "요청 파라미터가 올바르지 않습니다."
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 타입 변환 실패 ({@code @PathVariable}, {@code @RequestParam})
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "MethodArgumentTypeMismatchException");

        String message = String.format(
                "파라미터 '%s'의 값 '%s'을(를) %s 타입으로 변환할 수 없습니다.",
                e.getName(),
                e.getValue(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown"
        );

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * JSON 파싱 실패
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "HttpMessageNotReadableException");

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE,
                "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요."
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "IllegalArgumentException");

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * IllegalStateException 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "IllegalStateException");

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 인증 실패 (Spring Security)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "AuthenticationException");

        ErrorResponse response = ErrorResponse.of(ErrorCode.USER_UNAUTHORIZED);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    /**
     * 권한 없음 (Spring Security)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        logWarn(request, e, "AccessDeniedException");

        ErrorResponse response = ErrorResponse.of(ErrorCode.USER_FORBIDDEN);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * 그 외 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        logError(request, e, "UnhandledException");

        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    // ===== 로깅 헬퍼 =====

    private void logWarn(HttpServletRequest request, Exception e, String errorType) {
        String method = request.getMethod();
        String uri = getFullRequestPath(request);
        String userInfo = getCurrentUser();

        log.warn("[{}] | {} {} | User: {} | Error: {}",
                errorType, method, uri, userInfo, e.getMessage());
    }

    private void logError(HttpServletRequest request, Exception e, String errorType) {
        String method = request.getMethod();
        String uri = getFullRequestPath(request);
        String userInfo = getCurrentUser();

        log.error("[{}] | {} {} | User: {} | Error: {}",
                errorType, method, uri, userInfo, e.getMessage(), e);
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        return auth.getName();
    }

    private String getFullRequestPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        return queryString != null ? requestURI + "?" + queryString : requestURI;
    }
}
