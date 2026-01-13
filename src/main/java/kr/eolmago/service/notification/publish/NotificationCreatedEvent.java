package kr.eolmago.service.notification.publish;

import java.util.UUID;

public record NotificationCreatedEvent(
	UUID userId,
	String title,
	String body,
	String linkUrl
) {
	public String toChatContent() {
		if (linkUrl == null || linkUrl.isBlank()) {
			return title + "\n" + body;
		}
		return title + "\n" + body + "\n" + linkUrl;
	}
}
