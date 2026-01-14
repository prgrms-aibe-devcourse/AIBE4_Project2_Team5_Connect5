package kr.eolmago.controller.api.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 구매자 리뷰 조회 API 컨트롤러
 */
@Tag(name = "Buyer Review", description = "구매자 리뷰 조회 API")
@RestController
@RequestMapping("/api/buyer/reviews")
@RequiredArgsConstructor
public class BuyerReviewApiController {

    private final ReviewService reviewService;

    /**
     * 내가 작성한 리뷰 목록 (구매자 입장)
     */
    @Operation(summary = "구매자 작성 리뷰 목록 조회")
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getBuyerReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "filter", required = false, defaultValue = "all") String filter
    ) {
        UUID buyerId = userDetails.getUserId();

        // 구매자로서 작성한 리뷰 전체 조회
        List<ReviewResponse> reviews = reviewService.getReviewsByBuyer(buyerId);

        return ResponseEntity.ok(reviews);
    }
}