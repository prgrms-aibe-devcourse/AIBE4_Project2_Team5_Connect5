package kr.eolmago.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByAuctionAuctionIdAndRoomType(UUID auctionId, ChatRoomType roomType);

	Optional<ChatRoom> findByRoomTypeAndTargetUserId(ChatRoomType roomType, UUID targetUserId);

	@Query("""
        select r
        from ChatRoom r
        join fetch r.seller s
        join fetch r.buyer b
        left join fetch r.auction a
        where (s.userId = :userId or b.userId = :userId)
          and r.roomType = :roomType
        order by r.updatedAt desc
    """)
	List<ChatRoom> findMyRoomsByType(@Param("userId") UUID userId, @Param("roomType") ChatRoomType roomType);
}
