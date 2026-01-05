package kr.eolmago.dto.api.notification.response;

import java.time.OffsetDateTime;
import kr.eolmago.domain.entity.notification.Notification;
import kr.eolmago.domain.entity.notification.enums.*;
import lombok.Builder;

@Builder
public record NotificationResponse(
	Long notificationId,
	NotificationType type,
	String title,
	String body,
	String linkUrl,
	RelatedEntityType relatedEntityType,
	String relatedEntityId,
	boolean read,
	OffsetDateTime readAt,
	OffsetDateTime createdAt
) {
	public static NotificationResponse from(Notification n) {
		return NotificationResponse.builder()
			.notificationId(n.getNotificationId())
			.type(n.getType())
			.title(n.getTitle())
			.body(n.getBody())
			.linkUrl(n.getLinkUrl())
			.relatedEntityType(n.getRelatedEntityType())
			.relatedEntityId(n.getRelatedEntityId())
			.read(n.isRead())
			.readAt(n.getReadAt())
			.createdAt(n.getCreatedAt())
			.build();
	}
}
