package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionEndReason;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;
import kr.eolmago.global.util.TimeFormatter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AuctionDetailResponse(
        // auction
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
        String remainingTime,
        Integer durationHours,
        int extendCount,
        AuctionEndReason endReason,
        Long finalPrice,
        OffsetDateTime createdAt,

        // auction_item
        Long auctionItemId,
        String itemName,
        ItemCategory category,
        ItemCondition condition,
        Map<String, Object> specs,

        // user_profile
        UUID sellerId,
        String sellerNickname,
        int sellerTradeCount,

        // auction_image
        List<String> imageUrls,

        // bid
        UUID highestBidderId
) {
    public static AuctionDetailResponse from(AuctionDetailDto dto, List<String> imageUrls) {
        return new AuctionDetailResponse(
                dto.auctionId(),
                dto.title(),
                dto.description(),
                dto.status(),
                dto.startPrice(),
                dto.currentPrice(),
                dto.bidIncrement(),
                dto.bidCount(),
                dto.favoriteCount(),
                dto.startAt(),
                dto.endAt(),
                dto.originalEndAt(),
                TimeFormatter.formatRemainingTime(dto.endAt()),
                dto.durationHours(),
                dto.extendCount(),
                dto.endReason(),
                dto.finalPrice(),
                dto.createdAt(),
                dto.auctionItemId(),
                dto.itemName(),
                dto.category(),
                dto.condition(),
                dto.specs(),
                dto.sellerId(),
                dto.sellerNickname(),
                dto.sellerTradeCount(),
                imageUrls,
                dto.highestBidderId()
        );
    }
}
