package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;

import java.util.UUID;

public record AuctionDraftResponse(
        UUID sellerId,
        UUID auctionId,
        AuctionStatus status
) {
}