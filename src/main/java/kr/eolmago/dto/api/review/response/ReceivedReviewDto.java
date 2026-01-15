package kr.eolmago.dto.api.review.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceivedReviewDto(
        Long reviewId,
        Long dealId,
        int rating,
        String content,
        OffsetDateTime createdAt,
        UUID buyerId,
        String buyerNickname,
        String buyerProfileImageUrl
) {
}
