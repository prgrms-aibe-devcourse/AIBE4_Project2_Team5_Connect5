package kr.eolmago.service.notification;

import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.dto.api.notification.response.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

	public NotificationResponse toResponse(Notification n) {
		return NotificationResponse.from(n);
	}
}
