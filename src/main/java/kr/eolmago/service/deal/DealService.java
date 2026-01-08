package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.view.deal.DealResponse;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deal Service
 * 비즈니스 로직과 검증 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealService {

    private final DealRepository dealRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 거래 생성
     */
    @Transactional
    public DealResponse createDeal(
            UUID auctionId,
            UUID sellerId,
            UUID buyerId,
            Long finalPrice
    ) {
        // 비즈니스 검증: Entity 존재 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다"));
        User seller = userRepository.findByUserId(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다"));
        User buyer = userRepository.findByUserId(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다"));

        // 정적 팩토리 메서드로 생성
        Deal deal = Deal.create(
                auction,
                seller,
                buyer,
                finalPrice,
                OffsetDateTime.now().plusDays(7)
        );

        Deal saved = dealRepository.save(deal);
        return DealResponse.from(saved);
    }

    /**
     * 거래 조회
     */
    public DealResponse getDeal(Long dealId) {
        Deal deal = findDealById(dealId);
        return DealResponse.from(deal);
    }

    /**
     * 전체 거래 목록
     */
    public List<DealResponse> getAllDeals() {
        return dealRepository.findAll().stream()
                .map(DealResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상태별 거래 목록
     */
    public List<DealResponse> getDealsByStatus(DealStatus status) {
        return dealRepository.findByStatus(status).stream()
                .map(DealResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 판매자 확인
     */
    @Transactional
    public DealResponse confirmBySeller(Long dealId) {
        Deal deal = findDealById(dealId);

        if (deal.getSellerConfirmedAt() != null) {
            throw new IllegalStateException("이미 확인했습니다");
        }

        deal.confirmBySeller();
        return DealResponse.from(deal);
    }

    /**
     * 구매자 확인
     */
    @Transactional
    public DealResponse confirmByBuyer(Long dealId) {
        Deal deal = findDealById(dealId);

        if (deal.getBuyerConfirmedAt() != null) {
            throw new IllegalStateException("이미 확인했습니다");
        }

        deal.confirmByBuyer();
        return DealResponse.from(deal);
    }

    /**
     * 거래 완료
     */
    @Transactional
    public DealResponse completeDeal(Long dealId) {
        Deal deal = findDealById(dealId);

        if (!deal.canComplete()) {
            throw new IllegalStateException("완료할 수 없는 상태입니다");
        }

        deal.complete();
        return DealResponse.from(deal);
    }

    /**
     * 거래 종료 (취소)
     */
    @Transactional
    public DealResponse terminateDeal(Long dealId, String reason) {
        Deal deal = findDealById(dealId);

        if (!deal.canTerminate()) {
            throw new IllegalStateException("종료할 수 없는 상태입니다");
        }

        deal.terminate(reason);
        return DealResponse.from(deal);
    }

    /**
     * 거래 만료
     */
    @Transactional
    public DealResponse expireDeal(Long dealId) {
        Deal deal = findDealById(dealId);
        deal.expire();
        return DealResponse.from(deal);
    }

    // ========================================
    // Private Helper
    // ========================================

    private Deal findDealById(Long dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다"));
    }
}
