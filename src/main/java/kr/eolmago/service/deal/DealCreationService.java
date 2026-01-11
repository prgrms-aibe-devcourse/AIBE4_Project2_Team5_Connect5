package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.deal.request.CreateDealFromAuctionRequest;
import kr.eolmago.dto.api.deal.response.DealCreationResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 거래 생성 전담 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealCreationService {

    private final DealRepository dealRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 경매로부터 거래 생성
     */
    @Transactional
    public DealCreationResponse createDealFromAuction(CreateDealFromAuctionRequest request) {

        UUID auctionId = request.auctionId();
        UUID sellerId = request.sellerId();
        UUID buyerId = request.buyerId();
        Long finalPrice = request.finalPrice();

        // 경매 존재 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // 판매자 확인
        User seller = userRepository.findByUserId(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 구매자 확인
        User buyer = userRepository.findByUserId(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 중복 생성 방지 체크
        if (dealRepository.existsByAuction(auction)) {
            throw new BusinessException(ErrorCode.DEAL_ALREADY_EXISTS);
        }

        // 확정 기한 계산 (7일)
        OffsetDateTime confirmByAt = OffsetDateTime.now().plusDays(7);

        // Deal 생성
        Deal deal = Deal.create(
                auction,
                seller,
                buyer,
                finalPrice,
                confirmByAt
        );

        Deal savedDeal = dealRepository.save(deal);

        return new DealCreationResponse(
                true,
                savedDeal.getDealId(),
                auctionId,
                "거래가 생성되었습니다"
        );
    }

    /**
     * 경로 파라미터 방식의 거래 생성 (auctionId 검증 포함)
     */
    @Transactional
    public DealCreationResponse createDealFromAuctionPath(UUID auctionId, CreateDealFromAuctionRequest request) {
        // auctionId 일치 검증
        if (!auctionId.equals(request.auctionId())) {
            throw new BusinessException(ErrorCode.AUCTION_ID_MISMATCH);
        }

        return createDealFromAuction(request);
    }

    /**
     * 특정 경매의 거래 존재 여부 확인
     */
    public boolean isDealExistsForAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .map(dealRepository::existsByAuction)
                .orElse(false);
    }
}
