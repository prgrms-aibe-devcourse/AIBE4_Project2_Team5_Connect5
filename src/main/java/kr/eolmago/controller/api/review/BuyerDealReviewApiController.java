package kr.eolmago.controller.api.review;

import kr.eolmago.dto.api.review.request.ReviewCreateRequest;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 구매자 거래 상세(구매 거래 목록 → 리뷰 작성 버튼)에서 사용하는
 * "거래별 리뷰 작성" 전용 컨트롤러.
 *
 * buyer-review_create.js 에서 호출하는
 *   POST /api/buyer/deals/{dealId}/review
 * 엔드포인트를 처리한다.
 */
@RestController
@RequiredArgsConstructor
public class BuyerDealReviewApiController {

    private final ReviewService reviewService;

    @PostMapping("/api/buyer/deals/{dealId}/review")
    public ResponseEntity<Void> createBuyerDealReview(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReviewCreateRequest request
    ) {
        UUID reviewerId = userDetails.getUserId();

        // ReviewService의 공용 메서드 사용
        reviewService.createReview(dealId, reviewerId, request);

        // 프론트는 상태 코드만 확인하므로 body 없이 200 OK만 내려주면 충분
        return ResponseEntity.ok().build();
    }
}