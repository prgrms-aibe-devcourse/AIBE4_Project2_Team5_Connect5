package kr.eolmago.controller.api.review;

import kr.eolmago.dto.api.review.response.ReceivedReviewListResponse;
import kr.eolmago.service.review.ReviewQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserReviewApiController {

    private final ReviewQueryService reviewQueryService;

    /**
     * 로그인한 사용자라면 누구나(인증된 사용자) 특정 판매자가 "받은 리뷰" 목록 조회
     * GET /api/users/{userId}/reviews/received?page=0&size=10
     */
    @GetMapping("/{userId}/reviews/received")
    public ResponseEntity<ReceivedReviewListResponse> getReceivedReviews(
            @PathVariable("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // page/size 방어 (원하면 더 엄격히 제한 가능)
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 50) size = 50;

        ReceivedReviewListResponse response = reviewQueryService.getReceivedReviews(userId, page, size);
        return ResponseEntity.ok(response);
    }
}
