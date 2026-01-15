package kr.eolmago.repository.chat;

import java.util.Optional;
import java.util.UUID;

import io.lettuce.core.dynamic.annotation.Param;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

	Optional<ChatRoom> findByAuctionAuctionIdAndRoomType(UUID auctionId, ChatRoomType roomType);

	Optional<ChatRoom> findByRoomTypeAndTargetUserId(ChatRoomType roomType, UUID targetUserId);
}
