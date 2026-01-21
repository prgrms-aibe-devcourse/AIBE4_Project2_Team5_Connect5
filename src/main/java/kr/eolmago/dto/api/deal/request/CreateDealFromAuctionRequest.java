package kr.eolmago.dto.api.deal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDealFromAuctionRequest(

        @NotNull(message = "경매 ID는 필수입니다")
        UUID auctionId,

        @NotNull(message = "판매자 ID는 필수입니다")
        UUID sellerId,

        @NotNull(message = "구매자 ID는 필수입니다")
        UUID buyerId,

        @NotNull(message = "낙찰가는 필수입니다")
        @Min(value = 0, message = "낙찰가는 0 이상이어야 합니다")
        Long finalPrice
) {
}
