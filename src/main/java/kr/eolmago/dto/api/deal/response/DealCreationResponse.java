package kr.eolmago.dto.api.deal.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * 거래 생성 응답 DTO
 */
@Schema(description = "거래 생성 응답")
public record DealCreationResponse(
        
        @Schema(description = "성공 여부", example = "true")
        boolean success,
        
        @Schema(description = "생성된 거래 ID", example = "123")
        Long dealId,
        
        @Schema(description = "경매 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID auctionId,
        
        @Schema(description = "메시지", example = "거래가 생성되었습니다")
        String message
) {
}
