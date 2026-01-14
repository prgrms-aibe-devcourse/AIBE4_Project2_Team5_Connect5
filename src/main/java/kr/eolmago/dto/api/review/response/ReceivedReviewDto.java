package kr.eolmago.dto.api.review.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReceivedReviewDto {

    private Long reviewId;
    private Long dealId;

    private int rating;
    private String content;
    private OffsetDateTime createdAt;

    private UUID buyerId;
    private String buyerNickname;
    private String buyerProfileImageUrl;
}
