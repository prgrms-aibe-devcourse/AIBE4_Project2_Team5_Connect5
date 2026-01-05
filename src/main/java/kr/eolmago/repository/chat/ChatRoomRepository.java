package kr.eolmago.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.lettuce.core.dynamic.annotation.Param;
import kr.eolmago.domain.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByAuctionAuctionId(UUID auctionId);

	//Todo: Dsl로 변경할 예정
	@Query("""
        select r
        from ChatRoom r
        join fetch r.auction a
        join fetch r.seller s
        join fetch r.buyer b
        where s.userId = :userId or b.userId = :userId
        order by r.updatedAt desc
    """)
	List<ChatRoom> findMyRooms(@Param("userId") UUID userId);
}
