package kr.eolmago.service.notification.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode {

	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "NOTIF_AUTH_001", "로그인이 필요합니다."),
	INVALID_AUTH(HttpStatus.UNAUTHORIZED, "NOTIF_AUTH_002", "인증 정보가 올바르지 않습니다."),

	INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "NOTIF_REQ_001", "페이지 요청이 올바르지 않습니다."),

	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIF_404", "알림을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	NotificationErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
