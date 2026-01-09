package kr.eolmago.dto.api.deal;

import kr.eolmago.dto.view.deal.DealResponse;

import java.time.OffsetDateTime;

/**
 * 판매자 거래 목록용 API 응답 DTO
 */
public record SellerDealListDto(
        Long dealId,
        String auctionTitle,
        String buyerName,
        Long finalPrice,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime confirmByAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime completedAt,
        boolean sellerConfirmed,
        boolean buyerConfirmed
) {
    /**
     * DealResponse를 기반으로 간소화된 DTO 생성
     * 실제 구현시 buyerName은 별도로 조회 필요
     */
    public static SellerDealListDto from(DealResponse deal, String buyerName) {
        return new SellerDealListDto(
                deal.dealId(),
                deal.auctionTitle(),
                buyerName,
                deal.finalPrice(),
                deal.status(),
                deal.createdAt(),
                deal.confirmByAt(),
                deal.confirmedAt(),
                deal.completedAt(),
                deal.sellerConfirmedAt() != null,
                deal.buyerConfirmedAt() != null
        );
    }
}
