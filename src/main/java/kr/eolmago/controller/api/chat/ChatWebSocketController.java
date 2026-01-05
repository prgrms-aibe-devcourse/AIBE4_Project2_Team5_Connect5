package kr.eolmago.controller.api.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;
import java.util.UUID;

import kr.eolmago.dto.api.chat.request.ChatSendMessageRequest;
import kr.eolmago.service.chat.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Tag(name = "Chat (WebSocket)", description = "STOMP 기반 채팅 송신(문서용)")
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

	private final ChatCommandService chatCommandService;

	@Operation(
		summary = "채팅 메시지 전송(STOMP)",
		description = """
                    STOMP로 /app/chat.send 로 전송함.
                    서버는 Redis Stream 적재 → 컨슈머가 RDB 저장 후 /topic/chat.rooms.{roomId}로 브로드캐스트함.
                    """
	)
	@MessageMapping("/chat.send")
	public void send(ChatSendMessageRequest request, Principal principal) {
		UUID senderId = UUID.fromString(principal.getName());
		chatCommandService.publishMessage(request.roomId(), senderId, request.content());
	}
}
