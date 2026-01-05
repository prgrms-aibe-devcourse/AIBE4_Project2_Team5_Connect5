package kr.eolmago.dto.api.chat.request;

import java.util.UUID;

public record ChatSendMessageRequest(
	Long roomId,
	UUID senderId,
	String content
) {}
