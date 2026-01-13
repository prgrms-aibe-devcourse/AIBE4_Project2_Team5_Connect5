package kr.eolmago.dto.view.review;

import kr.eolmago.domain.entity.review.Review;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Review 응답 DTO
 */
public record ReviewResponse(
        Long reviewId,
        Long dealId,
        String dealTitle,
        Long dealFinalPrice,
        UUID sellerId,
        UUID buyerId,
        int rating,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getDeal().getDealId(),
                // 경매 제목: Auction → title 사용
                review.getDeal().getAuction().getTitle(),
                review.getDeal().getFinalPrice(),
                review.getSeller().getUserId(),
                review.getBuyer().getUserId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
