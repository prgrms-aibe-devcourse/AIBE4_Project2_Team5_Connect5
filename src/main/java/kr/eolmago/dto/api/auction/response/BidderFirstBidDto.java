package kr.eolmago.dto.api.auction.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BidderFirstBidDto(
        UUID bidderId,
        OffsetDateTime firstBidAt
) {
}