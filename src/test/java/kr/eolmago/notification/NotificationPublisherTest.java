package kr.eolmago.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.notification.response.NotificationResponse;
import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.notification.NotificationSseHub;
import kr.eolmago.service.notification.publish.NotificationCreatedEvent;
import kr.eolmago.service.notification.publish.NotificationPublishCommand;
import kr.eolmago.service.notification.publish.NotificationPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class NotificationPublisherTest {

	@Test
	@DisplayName("알림 발행: 저장 후 SSE push + 채팅 이벤트 발행")
	void givenCommand_whenPublish_thenSavePushAndPublishEvent() {
		// given
		NotificationRepository notificationRepository = mock(NotificationRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		NotificationSseHub sseHub = mock(NotificationSseHub.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

		NotificationPublisher sut = new NotificationPublisher(
			notificationRepository,
			userRepository,
			sseHub,
			eventPublisher
		);

		UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		NotificationPublishCommand cmd = NotificationPublishCommand.auctionEnded(userId, 10L);

		given(userRepository.getReferenceById(userId)).willReturn(mock(User.class));
		given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

		// when
		sut.publish(cmd);

		// then
		then(notificationRepository).should().save(any(Notification.class));

		ArgumentCaptor<NotificationResponse> pushCaptor = ArgumentCaptor.forClass(NotificationResponse.class);
		then(sseHub).should().push(eq(userId), pushCaptor.capture());
		assertThat(pushCaptor.getValue()).isNotNull();

		ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
		then(eventPublisher).should().publishEvent(eventCaptor.capture());

		NotificationCreatedEvent e = eventCaptor.getValue();
		assertThat(e.userId()).isEqualTo(userId);
		assertThat(e.title()).isEqualTo(cmd.title());
		assertThat(e.body()).isEqualTo(cmd.body());
	}
}
