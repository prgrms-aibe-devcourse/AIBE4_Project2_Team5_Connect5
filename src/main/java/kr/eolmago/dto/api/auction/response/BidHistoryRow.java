package kr.eolmago.dto.api.auction.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BidHistoryRow(
        Long bidId,
        OffsetDateTime bidAt,
        Integer amount,
        UUID bidderId
) {
}