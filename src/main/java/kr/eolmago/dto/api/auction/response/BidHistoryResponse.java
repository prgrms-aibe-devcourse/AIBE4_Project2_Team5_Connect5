package kr.eolmago.dto.api.auction.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BidHistoryResponse (
        UUID auctionId,
        int currentPrice,
        int bidCount,
        OffsetDateTime endAt,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        List<BidHistoryItemResponse> items
) {}