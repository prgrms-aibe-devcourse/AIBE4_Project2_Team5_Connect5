package kr.eolmago.repository.deal;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long>, DealRepositoryCustom {
    
    /**
     * 상태별 거래 목록
     */
    List<Deal> findByStatus(DealStatus status);
    
    /**
     * 판매자 ID로 거래 목록
     */
    List<Deal> findBySeller_UserId(UUID sellerId);
    
    /**
     * 구매자 ID로 거래 목록
     */
    List<Deal> findByBuyer_UserId(UUID buyerId);
    
    /**
     * 만료된 거래 목록
     */
    List<Deal> findByStatusAndConfirmByAtBefore(DealStatus status, OffsetDateTime now);

    /**
     * 특정 경매로 생성된 거래 존재 여부 확인
     */
    boolean existsByAuction(Auction auction);

    /**
     * 경매로 거래 조회
     */
    Optional<Deal> findByAuction(Auction auction);

    // 완료된 거래 수 (판매자로 참여한 건)
    int countByStatusAndSeller_UserId(DealStatus status, UUID sellerId);

    // 완료된 거래 수 (구매자로 참여한 건)
    int countByStatusAndBuyer_UserId(DealStatus status, UUID buyerId);
}
