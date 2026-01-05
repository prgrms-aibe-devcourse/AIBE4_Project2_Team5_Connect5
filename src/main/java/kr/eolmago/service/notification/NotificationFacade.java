package kr.eolmago.service.notification;

import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.domain.entity.notification.enums.*;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.notification.response.NotificationResponse;
import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFacade {

	private final UserRepository userRepository;
	private final NotificationRepository notificationRepository;
	private final NotificationSseRegistry sseRegistry;

	@Transactional
	public NotificationResponse notify(
		UUID receiverUserId,
		NotificationType type,
		String title,
		String body,
		String linkUrl,
		RelatedEntityType relatedEntityType,
		String relatedEntityId
	) {
		User user = userRepository.getReferenceById(receiverUserId);

		Notification saved = notificationRepository.save(
			Notification.create(user, type, title, body, linkUrl, relatedEntityType, relatedEntityId)
		);

		NotificationResponse response = NotificationResponse.from(saved);

		// SSE는 "best effort"로: 트랜잭션 내에서 보내도 보통 문제 없지만,
		// 더 깔끔히 하려면 AFTER_COMMIT 이벤트로 빼도 됨(원하면 그 버전도 줄게).
		sseRegistry.push(receiverUserId, response);

		return response;
	}

	@Transactional
	public void notifyChatMessage(UUID receiverUserId, Long roomId, String senderName, String messagePreview) {
		String title = "채팅 메시지";
		String body = senderName + ": " + messagePreview;
		String link = "/chat/rooms/" + roomId;

		notify(receiverUserId,
			NotificationType.CHAT_MESSAGE,
			title,
			body,
			link,
			RelatedEntityType.CHAT,
			String.valueOf(roomId));
	}
}
