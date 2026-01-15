package kr.eolmago.dto.api.review.response;

import java.util.List;
import java.util.UUID;

public record ReceivedReviewListResponse(
        UUID sellerId,
        double averageRating,
        long totalCount,
        int page,
        int size,
        int totalPages,
        long totalElements,
        List<ReceivedReviewDto> reviews
) {
}
