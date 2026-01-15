package kr.eolmago.dto.api.deal.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * 구매자 거래 상세 응답 DTO
 */
@Schema(description = "구매자 거래 상세 응답")
public record BuyerDealDetailResponse(

        @Schema(description = "거래 ID", example = "123")
        Long dealId,

        @Schema(description = "거래 금액", example = "1500000")
        Long finalPrice,

        @Schema(description = "거래 상태", example = "PENDING_CONFIRMATION")
        String status,

        @Schema(description = "생성일시", example = "2025-01-10T10:30:00+09:00")
        String createdAt,

        @Schema(description = "판매자 확정 여부")
        Boolean sellerConfirmed,

        @Schema(description = "구매자 확정 여부")
        Boolean buyerConfirmed,

        @Schema(description = "판매자 확정 시간")
        String sellerConfirmedAt,

        @Schema(description = "구매자 확정 시간")
        String buyerConfirmedAt,

        @Schema(description = "거래 확정 완료 시간")
        String confirmedAt,

        @Schema(description = "확정 기한")
        String confirmByAt,

        @Schema(description = "배송 기한")
        String shipByAt,

        @Schema(description = "거래 완료 시간")
        String completedAt,

        UUID auctionId,
        String auctionTitle,

        String itemName,
        String itemCategory,
        String itemCondition,
        String itemBrand,
        String itemStorage,

        UUID sellerId,
        UUID buyerId,
        String sellerNickname,
        String buyerNickname,

        String thumbnailUrl
) {
        public static BuyerDealDetailResponse from(DealDetailDto dto) {
                return new BuyerDealDetailResponse(
                        dto.dealId(),
                        dto.finalPrice(),
                        dto.status(),
                        dto.createdAt(),
                        dto.sellerConfirmed(),
                        dto.buyerConfirmed(),
                        dto.sellerConfirmedAt(),
                        dto.buyerConfirmedAt(),
                        dto.confirmedAt(),
                        dto.confirmByAt(),
                        dto.shipByAt(),
                        dto.completedAt(),
                        dto.auctionId(),
                        dto.auctionTitle(),
                        dto.itemName(),
                        dto.getItemCategory(),    // 한글 카테고리명
                        dto.getItemCondition(),   // 상품 상태 (S급 등)
                        dto.getItemBrand(),       // specs에서 추출
                        dto.getItemStorage(),     // specs에서 추출
                        dto.sellerId(),
                        dto.buyerId(),
                        dto.sellerNickname(),
                        dto.buyerNickname(),
                        dto.thumbnailUrl()
                );
        }
}
