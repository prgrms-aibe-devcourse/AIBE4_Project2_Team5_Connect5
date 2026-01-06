package kr.eolmago.service.notification.publish;

import java.util.UUID;
import kr.eolmago.domain.entity.notification.enums.NotificationType;
import kr.eolmago.domain.entity.notification.enums.RelatedEntityType;

public record NotificationPublishCommand(
	UUID userId,
	NotificationType type,
	String title,
	String body,
	String linkUrl,
	RelatedEntityType relatedEntityType,
	String relatedEntityId
) {

	public static NotificationPublishCommand auctionEnded(UUID userId, Long auctionId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.AUCTION_ENDED,
			"경매가 종료되었습니다",
			"경매가 종료되었습니다. 결과를 확인해 주세요.",
			"/auctions/" + auctionId,
			RelatedEntityType.AUCTION,
			String.valueOf(auctionId)
		);
	}

	public static NotificationPublishCommand bidOutbid(UUID userId, Long auctionId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.BID_OUTBID,
			"입찰가가 갱신되었습니다",
			"누군가 더 높은 금액으로 입찰했습니다.",
			"/auctions/" + auctionId,
			RelatedEntityType.AUCTION,
			String.valueOf(auctionId)
		);
	}

	public static NotificationPublishCommand dealConfirmed(UUID userId, Long dealId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.DEAL_CONFIRMED,
			"거래가 확정되었습니다",
			"거래가 확정되었습니다. 내역을 확인해 주세요.",
			"/deals/" + dealId,
			RelatedEntityType.DEAL,
			String.valueOf(dealId)
		);
	}

	public static NotificationPublishCommand reportReceived(UUID userId, Long reportId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.REPORT_RECEIVED,
			"신고가 접수되었습니다",
			"신고가 정상적으로 접수되었습니다.",
			"/reports/" + reportId,
			RelatedEntityType.REPORT,
			String.valueOf(reportId)
		);
	}

	public static NotificationPublishCommand chatMessage(UUID userId, Long roomId, String preview) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.CHAT_MESSAGE,
			"새 메시지",
			preview,
			"/chat/rooms/" + roomId,
			RelatedEntityType.CHAT,
			String.valueOf(roomId)
		);
	}
}
