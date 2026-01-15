package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.auction.AuctionImage;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.user.UserProfile;
import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.dto.api.deal.response.BuyerDealListResponse;
import kr.eolmago.dto.api.deal.response.DealDetailDto;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.AuctionImageRepository;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.review.ReviewRepository;
import kr.eolmago.repository.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerDealService {

    private final DealRepository dealRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReviewRepository reviewRepository;


    /**
     * 구매자의 모든 거래 조회
     */
    public BuyerDealListResponse getBuyerDeals(UUID buyerId) {
        List<Deal> deals = dealRepository.findByBuyer_UserId(buyerId);

        // ✅ hasReview 배치 계산
        Set<Long> reviewedDealIds = new HashSet<>();
        if (!deals.isEmpty()) {
            List<Long> dealIds = deals.stream().map(Deal::getDealId).toList();
            reviewedDealIds.addAll(reviewRepository.findReviewedDealIds(dealIds));
        }

        List<BuyerDealListResponse.DealDto> dealDtos = deals.stream()
                .map(deal -> toDealDto(deal, reviewedDealIds.contains(deal.getDealId())))
                .toList();

        return new BuyerDealListResponse(
                buyerId,
                deals.size(),
                dealDtos
        );
    }

    /**
     * 구매자의 특정 거래 상세 조회 (목록용)
     */
    public BuyerDealListResponse.DealDto getDealListDetail(Long dealId, UUID buyerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!deal.getBuyer().getUserId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        boolean hasReview = reviewRepository.existsByDeal_DealId(dealId);
        return toDealDto(deal, hasReview);
    }

    /**
     * 구매자의 특정 거래 상세 조회
     */
    public BuyerDealDetailResponse getDealDetail(Long dealId, UUID buyerId) {
        DealDetailDto dealDetail = dealRepository.findDetailById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!dealDetail.buyerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        return BuyerDealDetailResponse.from(dealDetail);
    }

    /**
     * 구매자 측 거래 확정
     */
    @Transactional
    public void confirmDeal(Long dealId, UUID buyerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!deal.getBuyer().getUserId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        deal.confirmByBuyer();
    }

    /**
     * 구매자 수령 확인 → 거래 완료(COMPLETED) 처리
     */
    @Transactional
    public void receiveConfirm(Long dealId, UUID buyerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!deal.getBuyer().getUserId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        if (!deal.canComplete()) {
            throw new BusinessException(ErrorCode.DEAL_INVALID_STATUS);
        }

        deal.complete();

        UserProfile sellerProfile = userProfileRepository
                .findByUser_UserId(deal.getSeller().getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        sellerProfile.incrementTradeCount();
    }

    /**
     * Deal 엔티티 → BuyerDealListResponse.DealDto 변환
     */
    private BuyerDealListResponse.DealDto toDealDto(Deal deal, boolean hasReview) {
        String createdAt = deal.getCreatedAt() != null
                ? deal.getCreatedAt().toString()
                : null;

        String auctionTitle = null;
        String thumbnailUrl = null;

        if (deal.getAuction() != null) {
            auctionTitle = deal.getAuction().getTitle();

            if (deal.getAuction().getAuctionItem() != null) {
                List<AuctionImage> images =
                        auctionImageRepository.findByAuctionItemOrderByDisplayOrder(
                                deal.getAuction().getAuctionItem()
                        );

                if (!images.isEmpty()) {
                    thumbnailUrl = images.get(0).getImageUrl();
                }
            }
        }

        return new BuyerDealListResponse.DealDto(
                deal.getDealId(),
                deal.getFinalPrice(),
                deal.getStatus().name(),
                createdAt,
                auctionTitle,
                thumbnailUrl,
                hasReview
        );
    }
}
