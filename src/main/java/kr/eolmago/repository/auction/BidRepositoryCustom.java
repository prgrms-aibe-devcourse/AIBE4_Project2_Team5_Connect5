package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.dto.api.auction.response.BidHistoryRow;
import kr.eolmago.dto.api.auction.response.BidderFirstBidDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepositoryCustom {

    // 기존 입찰 조회
    Optional<Bid> findByClientRequestIdAndBidderId(String clientRequestId, UUID bidderId);

    // 현재 최고가 입찰 조회
    Optional<Bid> findTopByAuctionOrderByAmountDescCreatedAtAsc(Auction auction);

    // 경매의 현재 최고 입찰자 ID 조회
    Optional<UUID> findTopBidderIdByAuction(Auction auction);

    // 경매에 입찰이 존재하는지 확인
    boolean existsByAuction(Auction auction);

    // 입찰 히스토리 페이징 조회
    Page<BidHistoryRow> findBidHistory(UUID auctionId, Pageable pageable);

    // 입찰자별 최초 입찰 시각 조회
    List<BidderFirstBidDto> findBidderOrder(UUID auctionId);
}
