package kr.eolmago.service.notification.exception;

public class NotificationNotFoundException extends NotificationException {
	public NotificationNotFoundException() {
		super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
	}
}
