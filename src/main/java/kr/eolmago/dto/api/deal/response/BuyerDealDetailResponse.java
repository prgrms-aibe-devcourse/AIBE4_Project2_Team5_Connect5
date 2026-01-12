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

        @Schema(description = "판매자 ID")
        UUID sellerId,

        @Schema(description = "판매자 닉네임")
        String sellerNickname
) {
}
