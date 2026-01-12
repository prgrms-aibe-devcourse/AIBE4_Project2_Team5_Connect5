package kr.eolmago.dto.view.auction;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuctionEndAtView(
        UUID auctionId,
        OffsetDateTime endAt
) {
}
