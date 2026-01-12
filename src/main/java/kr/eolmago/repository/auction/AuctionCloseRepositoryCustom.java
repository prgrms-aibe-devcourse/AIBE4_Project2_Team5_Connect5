package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.view.auction.AuctionEndAtView;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionCloseRepositoryCustom {

    Optional<Auction> findByIdForUpdate(UUID auctionId);

    List<AuctionEndAtView> findAllEndAtByStatus(AuctionStatus status);

    List<UUID> findIdsToClose(AuctionStatus status, OffsetDateTime now, Pageable pageable);
}
