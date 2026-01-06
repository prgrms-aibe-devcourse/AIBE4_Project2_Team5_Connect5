package kr.eolmago.service.notification;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.dto.api.notification.response.NotificationResponse;
import kr.eolmago.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationValidator notificationValidator;
	private final NotificationMapper notificationMapper;
	private final NotificationSseHub sseHub;

	private final Clock clock;

	@Transactional(readOnly = true)
	public Page<NotificationResponse> list(UUID userId, int page, int size) {
		notificationValidator.validatePageRequest(page, size);

		Pageable pageable = PageRequest.of(page, size);
		return notificationRepository
			.findByUser_UserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
			.map(notificationMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public long unreadCount(UUID userId) {
		return notificationRepository.countByUser_UserIdAndReadFalseAndDeletedFalse(userId);
	}

	@Transactional
	public void readOne(UUID userId, Long notificationId) {
		Notification notification = notificationValidator.validateAndGetOwnedActive(userId, notificationId);
		notification.markRead(now());
	}

	@Transactional
	public int readAll(UUID userId) {
		return notificationRepository.markAllRead(userId, now());
	}

	@Transactional
	public void delete(UUID userId, Long notificationId) {
		Notification notification = notificationValidator.validateAndGetOwnedActive(userId, notificationId);
		notification.softDelete(now());
	}

	@Transactional(readOnly = true)
	public SseEmitter connectStream(UUID userId) {
		return sseHub.connect(userId);
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(clock);
	}
}
