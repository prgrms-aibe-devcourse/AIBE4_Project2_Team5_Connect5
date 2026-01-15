package kr.eolmago.controller.api.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.review.request.ReviewCreateRequest;
import kr.eolmago.dto.api.review.response.ReceivedReviewListResponse;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Review", description = "리뷰 관련 API")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "구매자 작성 리뷰 목록 조회")
    @GetMapping("/api/buyer/reviews")
    public ResponseEntity<List<ReviewResponse>> getBuyerReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        return ResponseEntity.ok(reviewService.getReviewsByBuyer(buyerId));
    }

    @Operation(summary = "판매자 받은 리뷰 목록 조회")
    @GetMapping("/api/seller/reviews")
    public ResponseEntity<List<ReviewResponse>> getSellerReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        return ResponseEntity.ok(reviewService.getReviewsBySeller(sellerId));
    }

    @Operation(summary = "구매자 거래 리뷰 작성")
    @PostMapping("/api/buyer/deals/{dealId}/review")
    public ResponseEntity<Void> createBuyerDealReview(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReviewCreateRequest request
    ) {
        UUID reviewerId = userDetails.getUserId();
        reviewService.createReview(dealId, reviewerId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "리뷰 삭제", description = "리뷰 작성자(구매자) 또는 판매자만 삭제할 수 있습니다.")
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "거래에 대한 리뷰 존재 여부")
    @GetMapping("/api/reviews/deals/{dealId}/exists")
    public ResponseEntity<Map<String, Boolean>> existsReviewForDeal(@PathVariable Long dealId) {
        boolean hasReview = reviewService.existsReviewForDeal(dealId);
        return ResponseEntity.ok(Map.of("hasReview", hasReview));
    }

    @Operation(summary = "작성 리뷰 상세 조회")
    @GetMapping("/api/buyer/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> getBuyerReviewDetail(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        return ResponseEntity.ok(reviewService.getReviewDetailForBuyer(reviewId, buyerId));
    }

    @Operation(summary = "받은 리뷰 상세 조회")
    @GetMapping("/api/seller/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> getSellerReviewDetail(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        return ResponseEntity.ok(reviewService.getReviewDetailForSeller(reviewId, sellerId));
    }
}
