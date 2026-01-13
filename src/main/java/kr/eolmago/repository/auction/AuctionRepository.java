package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID>, AuctionRepositoryCustom {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Auction a
        set a.favoriteCount = a.favoriteCount + 1
        where a.auctionId = :auctionId
    """)
    void incrementFavoriteCount(@Param("auctionId") UUID auctionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Auction a
        set a.favoriteCount = 
            case when a.favoriteCount > 0 then a.favoriteCount - 1 else 0 end
        where a.auctionId = :auctionId
    """)
    void decrementFavoriteCount(@Param("auctionId") UUID auctionId);

    // 최신 카운트 조회
    @Query("""
        select a.favoriteCount
        from Auction a
        where a.auctionId = :auctionId
    """)
    int findFavoriteCountByAuctionId(@Param("auctionId") UUID auctionId);
}