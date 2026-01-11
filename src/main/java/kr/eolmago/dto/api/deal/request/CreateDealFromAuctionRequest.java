package kr.eolmago.dto.api.deal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 경매 종료 후 거래 생성 요청 DTO
 * 
 * 경매 팀에서 이 형식으로 데이터를 보내면 됩니다.
 */
@Schema(description = "경매로부터 거래 생성 요청")
public record CreateDealFromAuctionRequest(
        
        @Schema(description = "경매 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "경매 ID는 필수입니다")
        UUID auctionId,
        
        @Schema(description = "판매자 ID", example = "550e8400-e29b-41d4-a716-446655440001")
        @NotNull(message = "판매자 ID는 필수입니다")
        UUID sellerId,
        
        @Schema(description = "구매자 ID (낙찰자)", example = "550e8400-e29b-41d4-a716-446655440002")
        @NotNull(message = "구매자 ID는 필수입니다")
        UUID buyerId,
        
        @Schema(description = "낙찰가 (최종 거래 금액)", example = "1500000")
        @NotNull(message = "낙찰가는 필수입니다")
        @Min(value = 0, message = "낙찰가는 0 이상이어야 합니다")
        Long finalPrice
) {
}
