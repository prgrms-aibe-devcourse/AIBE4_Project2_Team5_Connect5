package kr.eolmago.service.notification.exception;

public class NotificationInvalidRequestException extends NotificationException {
	public NotificationInvalidRequestException() {
		super(NotificationErrorCode.INVALID_PAGE_REQUEST);
	}
}
