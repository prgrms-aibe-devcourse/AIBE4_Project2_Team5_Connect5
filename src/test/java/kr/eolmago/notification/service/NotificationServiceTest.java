package kr.eolmago.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;

import kr.eolmago.service.notification.NotificationService;
import kr.eolmago.service.notification.exception.NotificationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceTest {

	private NotificationServiceTestDoubles doubles;
	private NotificationService sut;

	@BeforeEach
	void setUp() {
		doubles = NotificationServiceTestDoubles.create();
		sut = new NotificationService(
			doubles.notificationRepository,
			doubles.notificationValidator,
			doubles.notificationMapper,
			doubles.sseHub,
			doubles.clock
		);
	}

	private NotificationScenario given() {
		return NotificationScenario.given(doubles);
	}

	@Test
	@DisplayName("알림 1건 읽음 처리: 존재하면 markRead 호출")
	void givenNotificationExists_whenReadOne_thenMarkRead() {
		// Given
		NotificationScenario s = given().notificationExists();

		// When
		sut.readOne(s.userId, s.notificationId);

		// Then
		ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(s.notification).markRead(captor.capture());
		assertThat(captor.getValue()).isNotNull();
	}

	@Test
	@DisplayName("알림 1건 읽음 처리: 없으면 NOT_FOUND")
	void givenNotificationMissing_whenReadOne_thenThrowNotFound() {
		// Given
		NotificationScenario s = given(); // repo 기본값 Optional.empty

		// When
		Throwable t = catchThrowable(() -> sut.readOne(s.userId, s.notificationId));

		// Then
		assertThat(t).isInstanceOf(NotificationNotFoundException.class);
	}

	@Test
	@DisplayName("알림 삭제: 존재하면 softDelete 호출")
	void givenNotificationExists_whenDelete_thenSoftDelete() {
		// Given
		NotificationScenario s = given().notificationExists();

		// When
		sut.delete(s.userId, s.notificationId);

		// Then
		ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(s.notification).softDelete(captor.capture());
		assertThat(captor.getValue()).isNotNull();
	}

	@Test
	@DisplayName("알림 삭제: 없으면 NOT_FOUND")
	void givenNotificationMissing_whenDelete_thenThrowNotFound() {
		// Given
		NotificationScenario s = given();

		// When
		Throwable t = catchThrowable(() -> sut.delete(s.userId, s.notificationId));

		// Then
		assertThat(t).isInstanceOf(NotificationNotFoundException.class);
	}

	@Test
	@DisplayName("알림 전체 읽음: repo 결과를 그대로 반환")
	void givenMarkAllReadReturn_whenReadAll_thenReturnCount() {
		// Given
		NotificationScenario s = given().markAllReadReturns(7);

		// When
		int count = sut.readAll(s.userId);

		// Then
		assertThat(count).isEqualTo(7);
		verify(doubles.notificationRepository).markAllRead(eq(s.userId), any(OffsetDateTime.class));
	}
}
