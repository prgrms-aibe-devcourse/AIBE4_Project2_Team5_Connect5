package kr.eolmago.service.chat.validation;

import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.chat.exception.ChatException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatValidator {

	public UUID requireUserId(CustomUserDetails me) {
		if (me == null || !StringUtils.hasText(me.getId())) {
			throw new ChatException(ErrorCode.CHAT_AUTH_REQUIRED);
		}
		try {
			return UUID.fromString(me.getId());
		} catch (IllegalArgumentException e) {
			throw new ChatException(ErrorCode.CHAT_INVALID_AUTH);
		}
	}

	public void validateParticipant(ChatRoom room, UUID userId) {
		boolean isSeller = room.getSeller().getUserId().equals(userId);
		boolean isBuyer = room.getBuyer().getUserId().equals(userId);
		if (!isSeller && !isBuyer) {
			throw new ChatException(ErrorCode.CHAT_FORBIDDEN_ROOM);
		}
	}

	public void validateSendRequest(Long roomId, UUID senderId, String content) {
		if (roomId == null || senderId == null || !StringUtils.hasText(content)) {
			throw new ChatException(ErrorCode.CHAT_INVALID_SEND_REQUEST);
		}
	}

	public UUID tryParseUserId(CustomUserDetails me) {
		if (me == null || !StringUtils.hasText(me.getId())) return null;
		try {
			return UUID.fromString(me.getId());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
