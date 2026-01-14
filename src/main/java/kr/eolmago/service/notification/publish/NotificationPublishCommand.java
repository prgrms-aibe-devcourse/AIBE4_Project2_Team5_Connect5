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

	private static NotificationPublishCommand auction(
		UUID userId,
		NotificationType type,
		String title,
		String body,
		String auctionIdStr
	) {
		return new NotificationPublishCommand(
			userId,
			type,
			title,
			body,
			"/auctions/" + auctionIdStr,
			RelatedEntityType.AUCTION,
			auctionIdStr
		);
	}

	// (기존) 경매 종료
	public static NotificationPublishCommand auctionEnded(UUID userId, Long auctionId) {
		return auction(
			userId,
			NotificationType.AUCTION_ENDED,
			"⏰ 경매가 종료되었습니다",
			"경매가 종료되었습니다.\n결과를 확인해 주세요.",
			String.valueOf(auctionId)
		);
	}

	public static NotificationPublishCommand auctionEnded(UUID userId, UUID auctionId) {
		return auction(
			userId,
			NotificationType.AUCTION_ENDED,
			"⏰ 경매가 종료되었습니다",
			"경매가 종료되었습니다.\n결과를 확인해 주세요.",
			auctionId.toString()
		);
	}

	// 판매자: 경매 등록 완료(게시)
	public static NotificationPublishCommand auctionPublished(UUID userId, UUID auctionId) {
		return auction(
			userId,
			NotificationType.AUCTION_PUBLISHED,
			"✅ 경매 등록이 완료되었습니다",
			"경매가 정상적으로 게시되었습니다.\n경매 페이지에서 확인해 주세요.",
			auctionId.toString()
		);
	}

	// 판매자: 낙찰 확정(마감)
	public static NotificationPublishCommand auctionSold(UUID userId, UUID auctionId, long finalPrice) {
		return auction(
			userId,
			NotificationType.AUCTION_SOLD,
			"🎯 낙찰이 확정되었습니다",
			"경매가 낙찰되었습니다.\n최종 낙찰가: " + finalPrice + "원",
			auctionId.toString()
		);
	}

	// 구매자: 내가 낙찰자
	public static NotificationPublishCommand auctionWon(UUID userId, UUID auctionId, long finalPrice) {
		return auction(
			userId,
			NotificationType.AUCTION_WON,
			"🎉 낙찰되었습니다",
			"축하합니다!\n최종 낙찰가: " + finalPrice + "원",
			auctionId.toString()
		);
	}

	// 판매자: 유찰
	public static NotificationPublishCommand auctionUnsold(UUID userId, UUID auctionId) {
		return auction(
			userId,
			NotificationType.AUCTION_UNSOLD,
			"😢 유찰되었습니다",
			"경매가 유찰되었습니다.\n재등록을 고려해 주세요.",
			auctionId.toString()
		);
	}

	// 판매자: 경매 취소
	public static NotificationPublishCommand auctionCanceled(UUID userId, UUID auctionId) {
		return auction(
			userId,
			NotificationType.AUCTION_CANCELED,
			"⚠️ 경매가 취소되었습니다",
			"경매가 판매자에 의해 취소되었습니다.",
			auctionId.toString()
		);
	}

	// 구매자: 입찰 성공 처리 결과
	public static NotificationPublishCommand bidAccepted(UUID userId, UUID auctionId, long amount) {
		return auction(
			userId,
			NotificationType.BID_ACCEPTED,
			"✅ 입찰이 처리되었습니다",
			"입찰이 정상적으로 반영되었습니다.\n입찰가: " + amount + "원",
			auctionId.toString()
		);
	}

	// (기존) 내가 최고가에서 밀림
	public static NotificationPublishCommand bidOutbid(UUID userId, Long auctionId) {
		return auction(
			userId,
			NotificationType.BID_OUTBID,
			"🔥 입찰가가 갱신되었습니다",
			"누군가 더 높은 금액으로 입찰했습니다.\n현재 경매 상황을 확인해 보세요.",
			String.valueOf(auctionId)
		);
	}

	public static NotificationPublishCommand bidOutbid(UUID userId, UUID auctionId) {
		return auction(
			userId,
			NotificationType.BID_OUTBID,
			"🔥 입찰가가 갱신되었습니다",
			"누군가 더 높은 금액으로 입찰했습니다.\n현재 경매 상황을 확인해 보세요.",
			auctionId.toString()
		);
	}

	// 관심/입찰자: 마감 결과(관전용) - 문구는 호출부에서 결과에 맞게 넣어도 됨
	public static NotificationPublishCommand auctionEndedWatching(UUID userId, UUID auctionId, String title, String body) {
		return auction(
			userId,
			NotificationType.AUCTION_ENDED_WATCHING,
			title,
			body,
			auctionId.toString()
		);
	}

	// 마감 후 채팅방 생성(양쪽 공통)
	public static NotificationPublishCommand chatRoomCreated(UUID userId, Long roomId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.CHAT_ROOM_CREATED,
			"💬 채팅방이 열렸습니다",
			"거래를 위한 채팅방이 생성되었습니다.\n대화방으로 이동해 주세요.",
			"/chats/rooms/" + roomId,
			RelatedEntityType.CHAT,
			String.valueOf(roomId)
		);
	}

	public static NotificationPublishCommand dealConfirmed(UUID userId, Long dealId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.DEAL_CONFIRMED,
			"✅ 거래가 확정되었습니다",
			"거래가 확정되었습니다.\n거래 내역을 확인해 주세요.",
			"/deals/" + dealId,
			RelatedEntityType.DEAL,
			String.valueOf(dealId)
		);
	}

	// 거래 완료(판매자/구매자 둘 다 호출)
	public static NotificationPublishCommand dealCompleted(UUID userId, Long dealId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.DEAL_COMPLETED,
			"🎉 거래가 완료되었습니다",
			"거래가 완료 처리되었습니다.\n거래 내역을 확인해 주세요.",
			"/deals/" + dealId,
			RelatedEntityType.DEAL,
			String.valueOf(dealId)
		);
	}

	// 거래 만료 임박(몇 시간 전)
	public static NotificationPublishCommand dealExpiringSoon(UUID userId, Long dealId, long hoursLeft) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.DEAL_EXPIRING_SOON,
			"⏳ 거래 만료가 임박했습니다",
			"거래 만료까지 약 " + hoursLeft + "시간 남았습니다.\n확인해 주세요.",
			"/deals/" + dealId,
			RelatedEntityType.DEAL,
			String.valueOf(dealId)
		);
	}

	public static NotificationPublishCommand reportReceived(UUID userId, Long reportId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.REPORT_RECEIVED,
			"📩 신고가 접수되었습니다",
			"신고가 정상적으로 접수되었습니다.\n검토 후 안내드리겠습니다.",
			"/reports/" + reportId,
			RelatedEntityType.REPORT,
			String.valueOf(reportId)
		);
	}

	// 조치 완료 -> 신고자에게
	public static NotificationPublishCommand reportActionCompleted(UUID reporterId, Long reportId) {
		return new NotificationPublishCommand(
			reporterId,
			NotificationType.REPORT_ACTION_COMPLETED,
			"✅ 신고 조치가 완료되었습니다",
			"신고 건에 대한 조치가 완료되었습니다.\n결과를 확인해 주세요.",
			"/reports/" + reportId,
			RelatedEntityType.REPORT,
			String.valueOf(reportId)
		);
	}

	// 기간 정지 -> 피신고자에게
	public static NotificationPublishCommand reportSuspended(UUID reportedUserId, Long reportId, int days) {
		String durationText = (days == 0) ? "영구" : (days + "일");
		return new NotificationPublishCommand(
			reportedUserId,
			NotificationType.REPORT_SUSPENDED,
			"🚫 이용이 제한되었습니다",
			"신고 조치로 인해 " + durationText + " 동안 이용이 제한됩니다.\n자세한 내용을 확인해 주세요.",
			"/reports/" + reportId,
			RelatedEntityType.REPORT,
			String.valueOf(reportId)
		);
	}

	// 기각 -> 신고자에게
	public static NotificationPublishCommand reportRejected(UUID reporterId, Long reportId) {
		return new NotificationPublishCommand(
			reporterId,
			NotificationType.REPORT_REJECTED,
			"ℹ️ 신고가 기각되었습니다",
			"검토 결과 신고가 기각되었습니다.\n자세한 내용을 확인해 주세요.",
			"/reports/" + reportId,
			RelatedEntityType.REPORT,
			String.valueOf(reportId)
		);
	}

	public static NotificationPublishCommand phoneVerified(UUID userId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.PHONE_VERIFIED,
			"✅ 전화번호 인증이 완료되었습니다",
			"이제부터 전체 서비스를 이용할 수 있습니다.",
			"/",
			RelatedEntityType.USER,
			userId.toString()
		);
	}

	public static NotificationPublishCommand welcome(UUID userId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.WELCOME,
			"🎉 환영합니다!",
			"회원가입이 완료되었습니다.\n지금 바로 경매를 시작해보세요.",
			"/auctions",
			RelatedEntityType.USER,
			userId.toString()
		);
	}

	public static NotificationPublishCommand chatMessage(UUID userId, Long roomId, String preview) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.CHAT_MESSAGE,
			"💬 새 메시지가 도착했습니다",
			preview,
			"/chats/rooms/" + roomId,
			RelatedEntityType.CHAT,
			String.valueOf(roomId)
		);
	}

	public static NotificationPublishCommand favoriteAdded(UUID userId, UUID auctionId) {
		return new NotificationPublishCommand(
			userId,
			NotificationType.FAVORITE_ADDED,
			"⭐ 관심 경매에 추가되었습니다",
			"관심 경매로 등록되었습니다.\n마감 결과를 알림으로 알려드릴게요.",
			"/auctions/" + auctionId,
			RelatedEntityType.AUCTION,
			auctionId.toString()
		);
	}
}
