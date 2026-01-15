package kr.eolmago.dto.api.deal.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "판매자 거래 목록 응답")
public record SellerDealListResponse(

        @Schema(description = "판매자 ID")
        UUID sellerId,

        @Schema(description = "총 거래 수")
        int totalCount,

        @Schema(description = "거래 목록")
        List<DealDto> deals
) {

    @Schema(description = "판매자 거래 단건 정보")
    public record DealDto(

            @Schema(description = "거래 ID", example = "123")
            Long dealId,

            @Schema(description = "거래 금액 (최종 가격)", example = "1500000")
            Long finalPrice,

            @Schema(description = "거래 상태", example = "PENDING_CONFIRMATION")
            String status,

            @Schema(description = "생성일시", example = "2025-01-10T10:30:00+09:00")
            String createdAt,

            @Schema(description = "경매 제목", example = "아이폰 15 프로")
            String auctionTitle,

            @Schema(description = "경매 썸네일 이미지 URL", example = "https://cdn.example.com/img/xxx.jpg")
            String thumbnailUrl,

            @Schema(description = "리뷰 존재 여부", example = "true")
            boolean hasReview
    ) {
    }
}
