package kr.eolmago.service.review;

import kr.eolmago.dto.api.review.response.ReceivedReviewListResponse;

import java.util.UUID;

public interface ReviewQueryService {

    /**
     * sellerId(=판매자 userId)가 받은 리뷰 목록 조회 (페이징)
     */
    ReceivedReviewListResponse getReceivedReviews(UUID sellerId, int page, int size);
}
