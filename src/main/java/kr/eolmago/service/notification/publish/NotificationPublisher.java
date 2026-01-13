package kr.eolmago.service.notification.publish;

import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.notification.response.NotificationResponse;
import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.notification.NotificationSseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final NotificationSseHub sseRegistry;

	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public Long publish(NotificationPublishCommand cmd) {
		UUID userId = cmd.userId();

		User user = userRepository.getReferenceById(userId);

		Notification saved = notificationRepository.save(
			Notification.create(
				user,
				cmd.type(),
				cmd.title(),
				cmd.body(),
				cmd.linkUrl(),
				cmd.relatedEntityType(),
				cmd.relatedEntityId()
			)
		);

		sseRegistry.push(userId, NotificationResponse.from(saved));

		eventPublisher.publishEvent(new NotificationCreatedEvent(
			userId, cmd.title(), cmd.body(), cmd.linkUrl()
		));

		return saved.getNotificationId();
	}
}
