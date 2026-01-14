package kr.eolmago.service.review;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.domain.entity.review.Review;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.review.request.ReviewCreateRequest;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.review.ReviewRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 리뷰 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DealRepository dealRepository;
    private final UserRepository userRepository;

    /**
     * 리뷰 생성
     *
     * - 거래 상태가 COMPLETED 인 경우에만 작성 가능
     * - 이미 해당 거래에 대해 동일한 작성자 역할(구매자/판매자)의 리뷰가 있으면 예외(필요 시 추가)
     */
    @Transactional
    public ReviewResponse createReview(
            Long dealId,
            UUID reviewerId,
            ReviewCreateRequest request
    ) {
        // 거래 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 거래 상태 검증: 완료가 아니면 리뷰 불가
        if (deal.getStatus() != DealStatus.COMPLETED) {
            // TODO: ErrorCode에 REVIEW_NOT_ALLOWED 같은 코드가 생기면 교체
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 사용자 조회
        User seller = userRepository.findById(deal.getSeller().getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User buyer = userRepository.findById(deal.getBuyer().getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 내가 이 거래의 참여자인지 검증
        if (!seller.getUserId().equals(reviewerId) && !buyer.getUserId().equals(reviewerId)) {
            // TODO: 나중에 REVIEW_NOT_ALLOWED 같은 에러 코드로 분리 가능
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 평점 (int)
        int rating = request.rating();

        Review review = Review.create(
                deal,
                seller,
                buyer,
                rating,
                request.content()
        );

        Review saved = reviewRepository.save(review);
        return ReviewResponse.from(saved);
    }

    /**
     * 특정 리뷰 단건 조회
     */
    public ReviewResponse getReview(Long reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                // TODO: ErrorCode에 REVIEW_NOT_FOUND가 생기면 교체
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 필요 시 권한 검증 추가 가능
        // if (!review.getSeller().getUserId().equals(userId)
        //         && !review.getBuyer().getUserId().equals(userId)) {
        //     throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        // }

        return ReviewResponse.from(review);
    }

    /**
     * 내가 구매자로 참여한 거래들에 대한 리뷰 목록 조회
     */
    public List<ReviewResponse> getReviewsByBuyer(UUID buyerId) {
        return reviewRepository.findByBuyer_UserId(buyerId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * 내가 판매자로 참여한 거래들에 대한 리뷰 목록 조회
     */
    public List<ReviewResponse> getReviewsBySeller(UUID sellerId) {
        return reviewRepository.findBySeller_UserId(sellerId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                // TODO: ErrorCode에 REVIEW_NOT_FOUND가 생기면 교체
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 작성자(구매자 또는 판매자)만 삭제 가능
        if (!review.getBuyer().getUserId().equals(userId)
                && !review.getSeller().getUserId().equals(userId)) {
            // TODO: 나중에 REVIEW_NOT_ALLOWED 같은 코드로 교체 가능
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        reviewRepository.delete(review);
    }
}
