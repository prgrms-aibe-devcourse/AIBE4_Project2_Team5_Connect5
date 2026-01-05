package kr.eolmago.controller.api.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import kr.eolmago.domain.entity.chat.ChatMessage;
import kr.eolmago.dto.api.chat.response.ChatMessageResponse;
import kr.eolmago.repository.chat.ChatMessageRepository;
import kr.eolmago.service.chat.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "채팅방/메시지 조회 및 채팅방 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {

	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomService chatRoomService;

	@Operation(
		summary = "채팅 메시지 조회(페이징)",
		description = """
                    채팅방의 메시지를 최신순으로 30개 조회함.
                    cursor를 주면 해당 messageId 미만을 이어서 조회함.
                    """
	)
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/rooms/{roomId}/messages")
	public List<ChatMessageResponse> messages(
		@Parameter(description = "채팅방 ID", example = "1", required = true)
		@PathVariable Long roomId,

		@Parameter(description = "페이징 커서(이 messageId 미만 조회)", example = "100")
		@RequestParam(required = false) Long cursor
	) {
		List<ChatMessage> page;

		if (cursor == null) {
			page = chatMessageRepository.findTop30ByChatRoomChatRoomIdOrderByChatMessageIdDesc(roomId);
		} else {
			page = chatMessageRepository
				.findTop30ByChatRoomChatRoomIdAndChatMessageIdLessThanOrderByChatMessageIdDesc(roomId, cursor);
		}

		return page.stream()
			.map(ChatMessageResponse::from)
			.collect(Collectors.toList());
	}

	@Operation(
		summary = "채팅방 생성(또는 기존 채팅방 반환)",
		description = """
                    경매(auctionId) 기준으로 채팅방을 생성하거나,
                    이미 존재하면 기존 채팅방 id를 반환함.
                    """
	)
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/rooms")
	public Long createRoom(
		@Parameter(description = "경매 ID(UUID)", required = true)
		@RequestParam UUID auctionId,
		Principal principal
	) {
		UUID buyerId = UUID.fromString(principal.getName());
		return chatRoomService.createOrGetRoomForWinner(auctionId);
	}
}
