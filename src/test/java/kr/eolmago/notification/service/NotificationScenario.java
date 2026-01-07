package kr.eolmago.notification.service;

import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.domain.entity.notification.enums.NotificationType;
import kr.eolmago.domain.entity.notification.enums.RelatedEntityType;
import kr.eolmago.domain.entity.user.User;

final class NotificationScenario {

	static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
	static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	static UUID userId() {
		return UUID.fromString("11111111-1111-1111-1111-111111111111");
	}

	static OffsetDateTime now() {
		return OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
	}

	final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
	final Long notificationId = 1L;

	final NotificationServiceTestDoubles doubles;

	final Notification notification = mock(Notification.class);

	private NotificationScenario(NotificationServiceTestDoubles doubles) {
		this.doubles = doubles;

		when(doubles.notificationRepository.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId))
			.thenReturn(Optional.empty());
	}

	static NotificationScenario given(NotificationServiceTestDoubles doubles) {
		return new NotificationScenario(doubles);
	}

	NotificationScenario notificationExists() {
		when(doubles.notificationRepository.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId))
			.thenReturn(Optional.of(notification));
		return this;
	}

	NotificationScenario realNotificationExists() {
		Notification real = realNotification(notificationId, userId);
		when(doubles.notificationRepository.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId))
			.thenReturn(Optional.of(real));
		return this;
	}

	NotificationScenario markAllReadReturns(int count) {
		when(doubles.notificationRepository.markAllRead(eq(userId), any(OffsetDateTime.class)))
			.thenReturn(count);
		return this;
	}

	private static Notification realNotification(Long notificationId, UUID userId) {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(userId);

		Notification n = Notification.create(
			user,
			NotificationType.CHAT_MESSAGE,
			"title",
			"body",
			"https://example.com",
			RelatedEntityType.CHAT,
			"room-1"
		);

		trySetField(n, "notificationId", notificationId);
		trySetField(n, "createdAt", now()); // AuditableEntity에 있을 때만 의미

		return n;
	}

	private static void trySetField(Object target, String fieldName, Object value) {
		Field f = findField(target.getClass(), fieldName);
		if (f == null) return;
		try {
			f.setAccessible(true);
			f.set(target, value);
		} catch (IllegalAccessException ignored) {}
	}

	private static Field findField(Class<?> type, String fieldName) {
		Class<?> t = type;
		while (t != null && t != Object.class) {
			try {
				return t.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				t = t.getSuperclass();
			}
		}
		return null;
	}
}
