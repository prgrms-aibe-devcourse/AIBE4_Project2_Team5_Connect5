package kr.eolmago.dto.view.review;

import kr.eolmago.domain.entity.review.Review;
import kr.eolmago.domain.entity.user.User;
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
        String sellerNickname,
        UUID buyerId,
        String buyerNickname,
        int rating,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ReviewResponse from(Review review) {
        String sellerNickname = extractNickname(review.getSeller());
        String buyerNickname = extractNickname(review.getBuyer());

        return new ReviewResponse(
                review.getReviewId(),
                review.getDeal().getDealId(),
                review.getDeal().getAuction().getTitle(),
                review.getDeal().getFinalPrice(),
                review.getSeller().getUserId(),
                sellerNickname,
                review.getBuyer().getUserId(),
                buyerNickname,
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
    private static String extractNickname(User user) {
        if (user == null) return "-";
        if (user.getUserProfile() == null) return "-";
        String nick = user.getUserProfile().getNickname();
        return (nick == null || nick.isBlank()) ? "-" : nick;
    }
}