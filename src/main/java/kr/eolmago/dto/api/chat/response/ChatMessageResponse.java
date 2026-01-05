package kr.eolmago.dto.api.chat.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatMessage;

public record ChatMessageResponse(
	Long messageId,
	Long roomId,
	UUID senderId,
	String content,
	OffsetDateTime createdAt
) {
	public static ChatMessageResponse from(ChatMessage message) {
		return new ChatMessageResponse(
			message.getChatMessageId(),
			message.getChatRoom().getChatRoomId(),
			message.getSender().getUserId(),
			message.getContent(),
			message.getCreatedAt()
		);
	}
}
