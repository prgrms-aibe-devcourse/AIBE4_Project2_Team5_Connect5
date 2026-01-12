package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionEndReason;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuctionDetailDto(
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
        String thumbnailUrl,

        // bid
        UUID highestBidderId
) {
    public AuctionDetailDto withHighestBidderId(UUID highestBidderId) {
        return new AuctionDetailDto(
                auctionId(),
                title(),
                description(),
                status(),
                startPrice(),
                currentPrice(),
                bidIncrement(),
                bidCount(),
                favoriteCount(),
                startAt(),
                endAt(),
                originalEndAt(),
                durationHours(),
                extendCount(),
                endReason(),
                finalPrice(),
                createdAt(),
                auctionItemId(),
                itemName(),
                category(),
                condition(),
                specs(),
                sellerId(),
                sellerNickname(),
                sellerTradeCount(),
                thumbnailUrl(),
                highestBidderId // 현재 최고 입찰자의 user_id
        );
    }
}
