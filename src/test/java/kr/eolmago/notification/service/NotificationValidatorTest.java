package kr.eolmago.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.service.notification.NotificationValidator;
import kr.eolmago.service.notification.exception.NotificationAuthenticationException;
import kr.eolmago.service.notification.exception.NotificationErrorCode;
import kr.eolmago.service.notification.exception.NotificationInvalidRequestException;
import kr.eolmago.service.notification.exception.NotificationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationValidatorTest {

	@Mock
	NotificationRepository notificationRepository;

	private NotificationValidator sut() {
		return new NotificationValidator(notificationRepository);
	}

	@Test
	@DisplayName("유저ID 파싱: 로그인 정보 없으면 UNAUTHORIZED")
	void givenNoPrincipal_whenValidateUserId_thenThrowUnauthorized() {
		// when & then
		assertThatThrownBy(() -> sut().validateAndGetUserId(null))
			.isInstanceOf(NotificationAuthenticationException.class)
			.satisfies(e -> {
				NotificationAuthenticationException ex = (NotificationAuthenticationException) e;
				assertThat(ex.getErrorCode()).isEqualTo(NotificationErrorCode.UNAUTHORIZED);
			});
	}

	@Test
	@DisplayName("유저ID 파싱: UUID 형식 아니면 INVALID_AUTH")
	void givenInvalidUuid_whenValidateUserId_thenThrowInvalidAuth() {
		// given
		CustomUserDetails me = mock(CustomUserDetails.class);
		given(me.getId()).willReturn("not-a-uuid");

		// when & then
		assertThatThrownBy(() -> sut().validateAndGetUserId(me))
			.isInstanceOf(NotificationAuthenticationException.class)
			.satisfies(e -> {
				NotificationAuthenticationException ex = (NotificationAuthenticationException) e;
				assertThat(ex.getErrorCode()).isEqualTo(NotificationErrorCode.INVALID_AUTH);
			});
	}

	@Test
	@DisplayName("페이지 검증: 정상 범위면 통과")
	void givenValidPage_whenValidatePage_thenOk() {
		assertThatCode(() -> sut().validatePageRequest(0, 20))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("페이지 검증: 범위 벗어나면 INVALID_REQUEST")
	void givenInvalidPage_whenValidatePage_thenThrowInvalidRequest() {
		assertThatThrownBy(() -> sut().validatePageRequest(-1, 20))
			.isInstanceOf(NotificationInvalidRequestException.class);

		assertThatThrownBy(() -> sut().validatePageRequest(0, 0))
			.isInstanceOf(NotificationInvalidRequestException.class);

		assertThatThrownBy(() -> sut().validatePageRequest(0, 101))
			.isInstanceOf(NotificationInvalidRequestException.class);
	}

	@Test
	@DisplayName("알림 조회 검증: 없으면 NOT_FOUND")
	void givenMissingNotification_whenGetOwnedActive_thenThrowNotFound() {
		// given
		UUID userId = NotificationScenario.userId();
		Long notificationId = 1L;

		given(notificationRepository.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> sut().validateAndGetOwnedActive(userId, notificationId))
			.isInstanceOf(NotificationNotFoundException.class);
	}

	@Test
	@DisplayName("알림 조회 검증: 있으면 엔티티 반환")
	void givenExistingNotification_whenGetOwnedActive_thenReturnEntity() {
		// given
		UUID userId = NotificationScenario.userId();
		Long notificationId = 1L;

		Notification n = mock(Notification.class);
		given(notificationRepository.findByNotificationIdAndUser_UserIdAndDeletedFalse(notificationId, userId))
			.willReturn(Optional.of(n));

		// when
		Notification result = sut().validateAndGetOwnedActive(userId, notificationId);

		// then
		assertThat(result).isSameAs(n);
	}
}
