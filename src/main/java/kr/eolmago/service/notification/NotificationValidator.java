package kr.eolmago.service.notification;

import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.service.notification.exception.NotificationAuthenticationException;
import kr.eolmago.service.notification.exception.NotificationErrorCode;
import kr.eolmago.service.notification.exception.NotificationInvalidRequestException;
import kr.eolmago.service.notification.exception.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class NotificationValidator {

	private static final int MIN_PAGE = 0;
	private static final int MIN_SIZE = 1;
	private static final int MAX_SIZE = 100;

	private final NotificationRepository notificationRepository;

	public UUID validateAndGetUserId(CustomUserDetails me) {
		if (me == null || !StringUtils.hasText(me.getId())) {
			throw new NotificationAuthenticationException(NotificationErrorCode.UNAUTHORIZED);
		}

		try {
			return UUID.fromString(me.getId());
		} catch (IllegalArgumentException e) {
			throw new NotificationAuthenticationException(NotificationErrorCode.INVALID_AUTH);
		}
	}

	public void validatePageRequest(int page, int size) {
		if (page < MIN_PAGE || size < MIN_SIZE || size > MAX_SIZE) {
			throw new NotificationInvalidRequestException();
		}
	}

	public Notification validateAndGetOwnedActive(UUID userId, Long notificationId) {
		return notificationRepository
			.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId)
			.orElseThrow(NotificationNotFoundException::new);
	}
}
