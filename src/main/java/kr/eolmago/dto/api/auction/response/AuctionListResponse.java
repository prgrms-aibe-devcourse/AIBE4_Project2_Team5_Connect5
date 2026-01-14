package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionEndReason;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.global.util.TimeFormatter;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuctionListResponse(
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
        AuctionEndReason endReason
) {
    public static AuctionListResponse from(AuctionListDto dto) {
        return new AuctionListResponse(
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
                dto.endReason()
        );
    }
}