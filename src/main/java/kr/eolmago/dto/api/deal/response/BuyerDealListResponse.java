package kr.eolmago.dto.api.deal.response;

import java.util.List;
import java.util.UUID;

public record BuyerDealListResponse(
        UUID buyerId,
        int dealCount,
        List<DealDto> deals
) {
    public record DealDto(
            Long dealId,
            Long finalPrice,
            String status,
            String createdAt,
            String buyerConfirmedAt // 구매자 확정 시간 추가
    ) {
    }
}
