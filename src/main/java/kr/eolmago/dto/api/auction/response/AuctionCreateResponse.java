package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;

import java.util.UUID;

public record AuctionCreateResponse(
        UUID userId,
        UUID auctionId,
        AuctionStatus status
) {
    public static AuctionCreateResponse of(UUID userId, UUID auctionId, AuctionStatus status) {
        return new AuctionCreateResponse(userId, auctionId, status);
    }
}
