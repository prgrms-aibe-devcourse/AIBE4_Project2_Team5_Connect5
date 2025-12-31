package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    /**
     * 상태별 경매 목록 조회 (페이징)
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
     * Page<Auction> auctions = auctionRepository.findByStatus(AuctionStatus.LIVE, pageable);
     */
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    /**
     * 전체 경매 목록 조회 (페이징)
     */
    Page<Auction> findAll(Pageable pageable);
}