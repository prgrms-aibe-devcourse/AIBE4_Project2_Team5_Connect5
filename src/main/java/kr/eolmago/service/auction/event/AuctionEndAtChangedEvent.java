package kr.eolmago.service.auction.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuctionEndAtChangedEvent(
        UUID auctionId,
        OffsetDateTime endAt
) {
}