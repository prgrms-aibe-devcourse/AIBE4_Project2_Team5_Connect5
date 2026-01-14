package kr.eolmago.dto.api.auction.response;

import java.time.OffsetDateTime;

public record BidHistoryItemResponse(
        Long bidId,
        OffsetDateTime bidAt,
        Integer amount,
        boolean amountVisible,
        String bidderLabel,
        boolean isMe
) {
}