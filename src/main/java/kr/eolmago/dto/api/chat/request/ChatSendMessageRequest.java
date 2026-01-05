package kr.eolmago.dto.api.chat.request;

public record ChatSendMessageRequest(
	Long roomId,
	String content
) {}
