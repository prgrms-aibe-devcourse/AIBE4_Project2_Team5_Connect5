package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.Bid;

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
}
