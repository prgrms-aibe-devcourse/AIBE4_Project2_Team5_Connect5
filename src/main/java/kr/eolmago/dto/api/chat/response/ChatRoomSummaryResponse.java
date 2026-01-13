package kr.eolmago.dto.api.chat.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;

public record ChatRoomSummaryResponse(
	Long roomId,
	UUID auctionId,
	String auctionTitle,
	UUID opponentId,
	Long lastMessageId,
	long unreadCount,
	OffsetDateTime updatedAt
) {
	public static ChatRoomSummaryResponse from(ChatRoom room, UUID me) {
		if (room.getRoomType() == ChatRoomType.NOTIFICATION) {
			return new ChatRoomSummaryResponse(
				room.getChatRoomId(),
				null,
				"알림 채팅",
				null,
				room.getLastMessageId(),
				0L,
				room.getUpdatedAt()
			);
		}

		boolean iAmSeller = room.getSeller().getUserId().equals(me);

		UUID opponentId = iAmSeller
			? room.getBuyer().getUserId()
			: room.getSeller().getUserId();

		Long lastReadId = iAmSeller
			? room.getSellerLastReadId()
			: room.getBuyerLastReadId();

		long lastRead = (lastReadId == null) ? 0L : lastReadId;
		long lastMsg = (room.getLastMessageId() == null) ? 0L : room.getLastMessageId();
		long unread = Math.max(0L, lastMsg - lastRead);

		return new ChatRoomSummaryResponse(
			room.getChatRoomId(),
			room.getAuction().getAuctionId(),
			room.getAuction().getTitle(),
			opponentId,
			room.getLastMessageId(),
			unread,
			room.getUpdatedAt()
		);
	}

}
