package kr.eolmago.controller.api.notification;

import kr.eolmago.domain.entity.notification.enums.NotificationType;
import kr.eolmago.domain.entity.notification.enums.RelatedEntityType;

public record DevNotificationRequest(
	NotificationType type,
	String title,
	String body,
	String linkUrl,
	RelatedEntityType relatedEntityType,
	String relatedEntityId
) {
}
