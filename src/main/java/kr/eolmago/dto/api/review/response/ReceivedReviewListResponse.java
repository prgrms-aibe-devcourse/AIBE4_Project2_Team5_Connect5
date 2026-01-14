package kr.eolmago.dto.api.review.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedReviewListResponse {

    private UUID sellerId;

    // ⭐ ReviewQueryServiceImpl에서 averageRating(...) 호출하므로 필드명이 반드시 averageRating 이어야 함
    private double averageRating;

    private long totalCount;

    private int page;
    private int size;

    private int totalPages;
    private long totalElements;

    private List<ReceivedReviewDto> reviews;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivedReviewDto {

        private Long reviewId;
        private Long dealId;

        private int rating;
        private String content;

        private OffsetDateTime createdAt;

        private UUID buyerId;
        private String buyerNickname;
        private String buyerProfileImageUrl;
    }
}
