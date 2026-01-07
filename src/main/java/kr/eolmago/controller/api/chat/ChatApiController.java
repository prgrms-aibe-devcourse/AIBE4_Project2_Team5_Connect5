package kr.eolmago.controller.api.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import kr.eolmago.dto.api.chat.response.ChatMessageResponse;
import kr.eolmago.dto.api.chat.response.ChatRoomSummaryResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.chat.ChatService;
import kr.eolmago.service.chat.validation.ChatValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "채팅방/메시지 조회 및 채팅방 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {

	private final ChatService chatService;
	private final ChatValidator chatValidator;

	@Operation(summary = "내 채팅방 목록 조회", description = "로그인한 사용자가 seller/buyer로 참여 중인 채팅방 목록을 최신순으로 조회함.")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/rooms")
	public List<ChatRoomSummaryResponse> myRooms(@AuthenticationPrincipal CustomUserDetails me) {
		UUID userId = chatValidator.requireUserId(me);
		return chatService.getMyRooms(userId);
	}

	@Operation(
		summary = "채팅 메시지 조회(페이징)",
		description = """
            채팅방의 메시지를 최신순으로 30개 조회함.
            cursor를 주면 해당 messageId 미만을 이어서 조회함.
            (본인 참여 채팅방만 조회 가능)
            """
	)
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/rooms/{roomId}/messages")
	public List<ChatMessageResponse> messages(
		@Parameter(description = "채팅방 ID", example = "1", required = true)
		@PathVariable Long roomId,
		@Parameter(description = "페이징 커서(이 messageId 미만 조회)", example = "100")
		@RequestParam(required = false) Long cursor,
		@AuthenticationPrincipal CustomUserDetails me
	) {
		UUID userId = chatValidator.requireUserId(me);
		return chatService.getMessages(userId, roomId, cursor);
	}

	@Operation(summary = "채팅방 생성(또는 기존 채팅방 반환)", description = "경매(auctionId) 기준으로 채팅방을 생성하거나 이미 존재하면 기존 채팅방 id를 반환함.")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/rooms")
	public Long createRoom(@RequestParam UUID auctionId, @AuthenticationPrincipal CustomUserDetails me) {
		UUID userId = chatValidator.requireUserId(me);
		return chatService.createOrGetRoom(auctionId, userId);
	}
}
