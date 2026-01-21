package kr.eolmago.dto.api.review.response;

import java.time.OffsetDateTime;

public record ReviewResponse(
        Long reviewId,
        Long dealId,
        String dealTitle,
        Long dealFinalPrice,
        int rating,
        String content,
        OffsetDateTime createdAt,
        String buyerNickname,
        String buyerProfileImageUrl,
        String sellerNickname
) {
}
