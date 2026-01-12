package kr.eolmago.dto.api.auction.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BidCreateResponse(
        Long bidId,
        UUID auctionId,
        int acceptedAmount,
        int currentHighestAmount,
        int minAcceptableAmount,
        OffsetDateTime endAt,
        boolean extensionApplied,
        UUID highestBidderId
) {
}