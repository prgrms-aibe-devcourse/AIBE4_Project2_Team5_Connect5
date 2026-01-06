package kr.eolmago.notification.service;

import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.service.notification.NotificationMapper;
import kr.eolmago.service.notification.NotificationSseHub;
import kr.eolmago.service.notification.NotificationValidator;

final class NotificationServiceTestDoubles {

	final NotificationRepository notificationRepository = mock(NotificationRepository.class);

	final NotificationValidator notificationValidator = new NotificationValidator(notificationRepository);

	final NotificationMapper notificationMapper = mock(NotificationMapper.class);
	final NotificationSseHub sseHub = mock(NotificationSseHub.class);

	final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

	static NotificationServiceTestDoubles create() {
		return new NotificationServiceTestDoubles();
	}

	private NotificationServiceTestDoubles() {}
}
