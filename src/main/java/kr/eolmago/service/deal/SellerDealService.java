package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.auction.AuctionImage;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.dto.api.deal.response.DealDetailDto;
import kr.eolmago.dto.api.deal.response.SellerDealDetailResponse;
import kr.eolmago.dto.api.deal.response.SellerDealListResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.AuctionImageRepository;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.review.ReviewRepository;
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
public class SellerDealService {

    private final DealRepository dealRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 판매자의 모든 거래 조회
     */
    public SellerDealListResponse getSellerDeals(UUID sellerId) {
        List<Deal> deals = dealRepository.findBySeller_UserId(sellerId);

        // hasReview 배치 계산
        Set<Long> reviewedDealIds = new HashSet<>();
        if (!deals.isEmpty()) {
            List<Long> dealIds = deals.stream().map(Deal::getDealId).toList();
            reviewedDealIds.addAll(reviewRepository.findReviewedDealIds(dealIds));
        }

        List<SellerDealListResponse.DealDto> dealDtos = deals.stream()
                .map(deal -> toDealDto(deal, reviewedDealIds.contains(deal.getDealId())))
                .toList();

        return new SellerDealListResponse(
                sellerId,
                deals.size(),
                dealDtos
        );
    }

    /**
     * 판매자의 특정 거래 상세 조회
     */
    public SellerDealListResponse.DealDto getDealListDetail(Long dealId, UUID sellerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!deal.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        boolean hasReview = reviewRepository.existsByDeal_DealId(dealId);
        return toDealDto(deal, hasReview);
    }

    /**
     * 판매자의 특정 거래 상세 조회
     */
    public SellerDealDetailResponse getDealDetail(Long dealId, UUID sellerId) {
        DealDetailDto dealDetail = dealRepository.findDetailById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!dealDetail.sellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        return SellerDealDetailResponse.from(dealDetail);
    }

    /**
     * 판매자 측 거래 확정
     */
    @Transactional
    public void confirmDeal(Long dealId, UUID sellerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!deal.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        deal.confirmBySeller();
    }

    private SellerDealListResponse.DealDto toDealDto(Deal deal, boolean hasReview) {
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

        return new SellerDealListResponse.DealDto(
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
