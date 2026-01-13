package kr.eolmago.service.notification.publish;

import kr.eolmago.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationChatAppender {

	private final ChatService chatService;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void on(NotificationCreatedEvent e) {
		try {
			chatService.publishNotificationMessage(e.userId(), e.toChatContent());
		} catch (Exception ex) {
			log.warn("[NOTI_CHAT] append failed. userId={}, err={}", e.userId(), ex.toString());
		}
	}
}
