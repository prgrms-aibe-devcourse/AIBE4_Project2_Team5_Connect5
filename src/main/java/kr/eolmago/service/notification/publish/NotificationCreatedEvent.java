package kr.eolmago.service.notification.publish;

import java.util.UUID;

public record NotificationCreatedEvent(
	UUID userId,
	String title,
	String body
) {
	public String toChatContent() {
		return title + "\n" + body;
	}
}
