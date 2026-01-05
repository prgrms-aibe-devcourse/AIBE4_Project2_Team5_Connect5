package kr.eolmago.repository.chat;

import java.util.Optional;
import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByAuctionAuctionId(UUID auctionId);
}
