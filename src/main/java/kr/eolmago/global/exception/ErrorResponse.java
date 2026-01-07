package kr.eolmago.global.exception;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 에러 응답 DTO
 */
@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final OffsetDateTime timestamp;
    private final List<FieldError> errors;

    private ErrorResponse(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.timestamp = OffsetDateTime.now();
        this.errors = new ArrayList<>();
    }

    private ErrorResponse(ErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message;
        this.timestamp = OffsetDateTime.now();
        this.errors = new ArrayList<>();
    }

    private ErrorResponse(ErrorCode errorCode, List<FieldError> errors) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.timestamp = OffsetDateTime.now();
        this.errors = errors;
    }

    private ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = OffsetDateTime.now();
        this.errors = new ArrayList<>();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode, message);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode, errors);
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String value;
        private final String reason;

        private FieldError(String field, String value, String reason) {
            this.field = field;
            this.value = value;
            this.reason = reason;
        }

        public static FieldError of(String field, String value, String reason) {
            return new FieldError(field, value, reason);
        }
    }
}
