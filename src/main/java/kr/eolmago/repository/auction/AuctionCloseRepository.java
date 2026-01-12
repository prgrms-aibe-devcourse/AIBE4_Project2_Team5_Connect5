package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuctionCloseRepository extends JpaRepository<Auction, UUID>, AuctionCloseRepositoryCustom {

    List<Auction> findByStatusAndEndAtBefore(AuctionStatus status, OffsetDateTime endAt);

}
