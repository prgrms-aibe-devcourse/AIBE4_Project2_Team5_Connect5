package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionEndReason;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuctionDetailDto(
        // 경매 기본 정보
        UUID auctionId,
        String title,
        String description,
        AuctionStatus status,
        Integer startPrice,
        Integer currentPrice,
        Integer bidIncrement,
        int bidCount,
        int favoriteCount,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime originalEndAt,
        Integer durationHours,
        int extendCount,
        AuctionEndReason endReason,
        Long finalPrice,
        OffsetDateTime createdAt,

        // 상품 정보
        Long auctionItemId,
        String itemName,
        ItemCategory category,
        ItemCondition condition,
        Map<String, Object> specs,

        // 판매자 정보
        UUID sellerId,
        String sellerNickname,
        int sellerTradeCount,

        // 이미지 URL (첫 번째만)
        String thumbnailUrl
) {
}
