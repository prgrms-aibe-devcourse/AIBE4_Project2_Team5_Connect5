package kr.eolmago.service.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

	private final NotificationRepository notificationRepository;

	public void readOne(UUID userId, Long notificationId) {
		Notification notification = notificationRepository
			.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));

		notification.markRead(OffsetDateTime.now());
	}

	public int readAll(UUID userId) {
		return notificationRepository.markAllRead(userId, OffsetDateTime.now());
	}

	public void delete(UUID userId, Long notificationId) {
		Notification notification = notificationRepository
			.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));

		notification.softDelete(OffsetDateTime.now());
	}
}
