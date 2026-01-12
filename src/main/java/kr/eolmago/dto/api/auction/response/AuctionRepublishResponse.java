package kr.eolmago.dto.api.auction.response;

import java.util.UUID;

public record AuctionRepublishResponse(
        UUID newAuctionId,
        UUID originalAuctionId
) {
}