package kr.eolmago.repository.favorite;

import kr.eolmago.domain.entity.auction.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, Long>, FavoriteRepositoryCustom {

    Optional<Favorite> findByUser_UserIdAndAuction_AuctionId(UUID userId, UUID auctionId);

    boolean existsByUser_UserIdAndAuction_AuctionId(UUID userId, UUID auctionId);

    void deleteByUser_UserIdAndAuction_AuctionId(UUID userId, UUID auctionId);

    // 배치 찜 여부 확인용: 찜된 auctionId들만 뽑아오기
    @Query("""
        select f.auction.auctionId
        from Favorite f
        where f.user.userId = :userId
          and f.auction.auctionId in :auctionIds
    """)
    List<UUID> findFavoritedAuctionIds(
            @Param("userId") UUID userId,
            @Param("auctionIds") List<UUID> auctionIds
    );

    @Query("""
        select f.user.userId
        from Favorite f
        where f.auction.auctionId = :auctionId
    """)
    List<UUID> findUserIdsByAuctionId(@Param("auctionId") UUID auctionId);
}
