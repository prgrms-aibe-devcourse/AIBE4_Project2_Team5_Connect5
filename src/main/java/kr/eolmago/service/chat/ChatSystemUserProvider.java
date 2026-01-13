package kr.eolmago.service.chat;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatSystemUserProvider {

	private final UUID notificationBotUserId;

	public ChatSystemUserProvider(@Value("${chat.notification-bot-user-id}") String notificationBotUserId) {
		this.notificationBotUserId = UUID.fromString(notificationBotUserId);
	}

	public UUID notificationBotUserId() {
		return notificationBotUserId;
	}
}
