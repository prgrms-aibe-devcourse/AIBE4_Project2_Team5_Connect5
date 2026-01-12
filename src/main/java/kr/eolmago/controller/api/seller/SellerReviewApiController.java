package kr.eolmago.controller.api.seller;

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
 * 판매자 리뷰 조회 API 컨트롤러
 */
@Tag(name = "Seller Review", description = "판매자 리뷰 조회 API")
@RestController
@RequestMapping("/api/seller/reviews")
@RequiredArgsConstructor
public class SellerReviewApiController {

    private final ReviewService reviewService;

    /**
     * 판매자로서 받은 리뷰 목록
     */
    @Operation(summary = "판매자 받은 리뷰 목록 조회")
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getSellerReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "filter", required = false, defaultValue = "all") String filter
    ) {
        UUID sellerId = userDetails.getUserId();

        // 판매자가 받은 리뷰 전체 조회
        List<ReviewResponse> reviews = reviewService.getReviewsBySeller(sellerId);

        return ResponseEntity.ok(reviews);
    }
}
