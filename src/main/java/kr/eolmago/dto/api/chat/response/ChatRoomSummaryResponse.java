package kr.eolmago.dto.api.chat.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatRoomType;
import kr.eolmago.repository.chat.ChatRoomSummaryProjection;

public record ChatRoomSummaryResponse(
	Long roomId,
	String roomType,
	UUID auctionId,
	String auctionTitle,
	String thumbnailUrl,
	String lastMessage,
	OffsetDateTime lastMessageAt,
	long unreadCount
) {
	public static ChatRoomSummaryResponse from(ChatRoomSummaryProjection p) {
		Long unread = p.unreadCount();
		return new ChatRoomSummaryResponse(
			p.roomId(),
			p.roomType() == null ? null : p.roomType().name(),
			p.roomType() == ChatRoomType.NOTIFICATION ? null : p.auctionId(),
			p.roomType() == ChatRoomType.NOTIFICATION ? "알림 채팅" : p.auctionTitle(),
			p.thumbnailUrl(),
			p.lastMessage(),
			p.lastMessageAt(),
			unread == null ? 0L : unread
		);
	}
}
