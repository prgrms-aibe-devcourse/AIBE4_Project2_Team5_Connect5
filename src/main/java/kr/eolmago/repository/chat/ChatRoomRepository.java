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

	@Query("""
		select r
		from ChatRoom r
		join fetch r.seller s
		left join fetch r.buyer b
		left join fetch r.auction a
		where r.chatRoomId = :roomId
	""")
	Optional<ChatRoom> findRoomViewById(@Param("roomId") Long roomId);
}
