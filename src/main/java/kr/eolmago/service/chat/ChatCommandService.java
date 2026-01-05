package kr.eolmago.service.chat;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatCommandService {

	private final ChatStreamPublisher chatStreamPublisher;

	public void publishMessage(Long roomId, UUID senderId, String content) {
		if (roomId == null || senderId == null || !StringUtils.hasText(content)) {
			throw new IllegalArgumentException("invalid chat send request");
		}
		chatStreamPublisher.publish(roomId, senderId, content.trim());
	}
}
