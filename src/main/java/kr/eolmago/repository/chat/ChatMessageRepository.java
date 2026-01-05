package kr.eolmago.repository.chat;

import java.util.List;
import kr.eolmago.domain.entity.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findTop30ByChatRoomChatRoomIdOrderByChatMessageIdDesc(Long roomId);

	List<ChatMessage> findTop30ByChatRoomChatRoomIdAndChatMessageIdLessThanOrderByChatMessageIdDesc(
		Long roomId, Long cursor
	);
}
