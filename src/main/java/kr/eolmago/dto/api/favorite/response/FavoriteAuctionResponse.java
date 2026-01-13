package kr.eolmago.dto.api.favorite.response;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.global.util.TimeFormatter;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FavoriteAuctionResponse(
        UUID auctionId,
        Long auctionItemId,
        String itemName,
        String title,
        String thumbnailUrl,
        String sellerNickname,
        Integer startPrice,
        Integer currentPrice,
        Long finalPrice,
        int bidCount,
        int favoriteCount,
        OffsetDateTime endAt,
        String remainingTime,
        AuctionStatus status,
        OffsetDateTime favoritedAt,   // 찜한 시각
        boolean favorited             // 항상 true(찜 목록이니까)
) {
    public static FavoriteAuctionResponse from(FavoriteAuctionDto dto) {
        return new FavoriteAuctionResponse(
                dto.auctionId(),
                dto.auctionItemId(),
                dto.itemName(),
                dto.title(),
                dto.thumbnailUrl(),
                dto.sellerNickname(),
                dto.startPrice(),
                dto.currentPrice(),
                dto.finalPrice(),
                dto.bidCount(),
                dto.favoriteCount(),
                dto.endAt(),
                TimeFormatter.formatRemainingTime(dto.endAt()),
                dto.status(),
                dto.favoritedAt(),
                true // 찜 목록이므로 항상 true
        );
    }
}
