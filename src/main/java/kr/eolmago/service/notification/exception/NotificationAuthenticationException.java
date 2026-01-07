package kr.eolmago.service.notification.exception;

public class NotificationAuthenticationException extends NotificationException {
	public NotificationAuthenticationException(NotificationErrorCode errorCode) {
		super(errorCode);
	}
}
