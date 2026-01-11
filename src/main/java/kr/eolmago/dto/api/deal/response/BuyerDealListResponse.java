package kr.eolmago.dto.api.deal.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * 구매자 거래 목록 응답 DTO
 */
@Schema(description = "구매자 거래 목록 응답")
public record BuyerDealListResponse(
        
        @Schema(description = "구매자 ID")
        UUID buyerId,
        
        @Schema(description = "총 거래 수")
        int totalDeals,
        
        @Schema(description = "거래 목록")
        List<DealDto> deals
) {
    @Schema(description = "거래 정보")
    public record DealDto(
            @Schema(description = "거래 ID", example = "123")
            Long dealId,
            
            @Schema(description = "거래 금액", example = "1500000")
            Long finalPrice,
            
            @Schema(description = "거래 상태", example = "PENDING_CONFIRMATION")
            String status,
            
            @Schema(description = "생성일시", example = "2025-01-10T10:30:00+09:00")
            String createdAt
    ) {
    }
}
