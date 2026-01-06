package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID>, AuctionRepositoryCustom {

    // 상태별 경매 목록 조회
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);


    // 판매자별 경매 목록 조회
    Page<Auction> findBySeller(User seller, Pageable pageable);

    // 판매자 & 상태별 경매 목록 조회
    Page<Auction> findBySellerAndStatus(User seller, AuctionStatus status, Pageable pageable);
}