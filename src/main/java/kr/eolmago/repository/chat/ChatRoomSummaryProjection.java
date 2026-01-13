package kr.eolmago.repository.chat;

import java.time.OffsetDateTime;
import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatRoomType;

public record ChatRoomSummaryProjection(
	Long roomId,
	ChatRoomType roomType,
	UUID auctionId,
	String auctionTitle,
	String thumbnailUrl,
	String lastMessage,
	OffsetDateTime lastMessageAt,
	long unreadCount
) {}
