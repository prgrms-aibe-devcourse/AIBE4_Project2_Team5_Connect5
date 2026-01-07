package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;


public record AuctionListDto(
        UUID auctionId,
        Long auctionItemId,
        String title,
        String thumbnailUrl,
        String sellerNickname,
        Integer startPrice,
        Integer currentPrice,
        Long finalPrice,
        int bidCount,
        int viewCount,
        int favoriteCount,
        OffsetDateTime endAt,
        AuctionStatus status
) {
}
