package kr.eolmago.controller.api.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.review.request.ReviewCreateRequest;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "리뷰 API", description = "거래 리뷰 관련 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    @Operation(summary = "구매자가 리뷰 작성",
            description = "거래가 완료된 후, 구매자가 판매자에 대한 리뷰를 작성합니다.")
    @PostMapping("/deals/{dealId}")
    public ResponseEntity<ReviewResponse> createBuyerReview(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReviewCreateRequest request
    ) {
        UUID buyerId = userDetails.getUserId();

        ReviewResponse response = reviewService.createReview(dealId, buyerId, request);

        return ResponseEntity
                .created(URI.create("/api/reviews/" + response.reviewId()))
                .body(response);
    }

    @Operation(summary = "내가 작성한 리뷰 목록 조회 (구매자 기준)")
    @GetMapping("/me/buyer")
    public ResponseEntity<List<ReviewResponse>> getMyBuyerReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();

        List<ReviewResponse> responses = reviewService.getReviewsByBuyer(buyerId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "내가 받은 리뷰 목록 조회 (판매자 기준)")
    @GetMapping("/me/seller")
    public ResponseEntity<List<ReviewResponse>> getMySellerReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();

        List<ReviewResponse> responses = reviewService.getReviewsBySeller(sellerId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "리뷰 삭제", description = "리뷰 작성자(구매자) 또는 판매자만 삭제할 수 있습니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deal/{dealId}/exists")
    public ResponseEntity<Map<String, Boolean>> existsReviewForDeal(@PathVariable Long dealId) {
        boolean hasReview = reviewService.existsReviewForDeal(dealId);
        return ResponseEntity.ok(Map.of("hasReview", hasReview));
    }
}
