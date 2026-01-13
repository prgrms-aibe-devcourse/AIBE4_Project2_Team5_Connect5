package kr.eolmago.controller.view.chat;

import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.chat.ChatService;
import kr.eolmago.service.chat.validation.ChatValidator;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatViewController {

	private final ChatService chatService;
	private final ChatValidator chatValidator;

	@GetMapping
	public String chatList() {
		return "pages/chat/chat-list";
	}

	@GetMapping("/rooms/{roomId}")
	public String chatRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails me,
		Model model
	) {
		UUID userId = chatValidator.requireUserId(me);

		ChatRoom room = chatService.getRoomForUserOrThrow(userId, roomId);
		ChatRoomType roomType = room.getRoomType();
		boolean readOnly = (roomType == ChatRoomType.NOTIFICATION);

		String title;
		String subtitle;
		String auctionTitle;

		if (readOnly) {
			title = "알림 채팅";
			subtitle = "서비스 알림을 채팅 형태로 모아봐요";
			auctionTitle = null;
		} else {
			title = "채팅";
			subtitle = "거래 관련 대화를 나눠요";
			auctionTitle = room.getAuction() == null ? "" : room.getAuction().getTitle();
		}

		model.addAttribute("roomId", roomId);
		model.addAttribute("userId", userId.toString());
		model.addAttribute("roomType", roomType.name());
		model.addAttribute("readOnly", readOnly);
		model.addAttribute("title", title);
		model.addAttribute("subtitle", subtitle);
		model.addAttribute("auctionTitle", auctionTitle);

		return "pages/chat/chat-room";
	}
}
