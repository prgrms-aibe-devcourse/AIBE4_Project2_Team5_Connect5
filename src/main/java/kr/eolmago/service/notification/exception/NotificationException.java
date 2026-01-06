package kr.eolmago.service.notification.exception;

import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {

	private final NotificationErrorCode errorCode;

	protected NotificationException(NotificationErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
