package kr.eolmago.service.notification.publish;

import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.notification.response.NotificationResponse;
import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.service.notification.NotificationSseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.eolmago.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final NotificationSseHub sseRegistry;

	@Transactional
	public Long publish(NotificationPublishCommand cmd) {
		UUID userId = cmd.userId();

		User user = userRepository.getReferenceById(userId);

		Notification n = Notification.create(
			user,
			cmd.type(),
			cmd.title(),
			cmd.body(),
			cmd.linkUrl(),
			cmd.relatedEntityType(),
			cmd.relatedEntityId()
		);

		Notification saved = notificationRepository.save(n);

		sseRegistry.push(userId, NotificationResponse.from(saved));

		return saved.getNotificationId();
	}
}
