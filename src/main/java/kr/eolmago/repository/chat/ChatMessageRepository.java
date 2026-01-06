package kr.eolmago.repository.chat;

import java.util.List;
import kr.eolmago.domain.entity.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByChatRoomChatRoomIdOrderByChatMessageIdDesc(Long roomId, Pageable pageable);

	List<ChatMessage> findByChatRoomChatRoomIdAndChatMessageIdLessThanOrderByChatMessageIdDesc(
		Long roomId, Long cursor, Pageable pageable
	);
}
