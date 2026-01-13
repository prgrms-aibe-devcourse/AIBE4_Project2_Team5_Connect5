package kr.eolmago.controller.api.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.eolmago.dto.api.review.request.ReviewCreateRequest;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "리뷰 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성")
    @PostMapping("/deals/{dealId}")
    public ReviewResponse createReview(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ReviewCreateRequest request
    ) {
        UUID userId = userDetails.getUserId();
        return reviewService.createReview(dealId, userId, request.rating(), request.content());
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }
}
