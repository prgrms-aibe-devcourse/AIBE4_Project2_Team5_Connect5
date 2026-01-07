package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.global.util.TimeFormatter;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuctionListResponse(
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
        String remainingTime,
        AuctionStatus status
) {
    public static AuctionListResponse from(AuctionListDto dto) {
        return new AuctionListResponse(
                dto.auctionId(),
                dto.auctionItemId(),
                dto.title(),
                dto.thumbnailUrl(),
                dto.sellerNickname(),
                dto.startPrice(),
                dto.currentPrice(),
                dto.finalPrice(),
                dto.bidCount(),
                dto.viewCount(),
                dto.favoriteCount(),
                dto.endAt(),
                TimeFormatter.formatRemainingTime(dto.endAt()),
                dto.status()
        );
    }
}