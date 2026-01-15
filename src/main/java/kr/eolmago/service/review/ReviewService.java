package kr.eolmago.service.review;

import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DealRepository dealRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(
            Long dealId,
            UUID reviewerId,
            ReviewCreateRequest request
    ) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (deal.getStatus() != DealStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        User seller = userRepository.findById(deal.getSeller().getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User buyer = userRepository.findById(deal.getBuyer().getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!seller.getUserId().equals(reviewerId) && !buyer.getUserId().equals(reviewerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

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

    public ReviewResponse getReview(Long reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
        return ReviewResponse.from(review);
    }

    /**
     * (공통) dealId로 리뷰 상세 조회
     * - buyer 또는 seller만 조회 가능
     */
    public ReviewResponse getReviewDetailByDealId(Long dealId, UUID userId) {
        Review review = reviewRepository.findByDeal_DealId(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!review.getBuyer().getUserId().equals(userId)
                && !review.getSeller().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        return ReviewResponse.from(review);
    }

    /**
     * BuyerViewController가 호출하는 메서드 (시그니처 맞춤용)
     * - buyer만 조회 가능하게 강제
     */
    public ReviewResponse getReviewByDealIdForBuyer(Long dealId, UUID buyerId) {
        Review review = reviewRepository.findByDeal_DealIdAndBuyer_UserId(dealId, buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND)); // 프로젝트 에러코드 정책에 맞게 조정
        return ReviewResponse.from(review);
    }

    public ReviewResponse getReviewByDealIdForSeller(Long dealId, UUID sellerId) {
        Review review = reviewRepository.findByDeal_DealIdAndSeller_UserId(dealId, sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
        return ReviewResponse.from(review);
    }

    public List<ReviewResponse> getReviewsByBuyer(UUID buyerId) {
        return reviewRepository.findByBuyer_UserId(buyerId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    public List<ReviewResponse> getReviewsBySeller(UUID sellerId) {
        return reviewRepository.findBySeller_UserId(sellerId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public void deleteReview(Long reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        if (!review.getBuyer().getUserId().equals(userId)
                && !review.getSeller().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        reviewRepository.delete(review);
    }

    public boolean existsReviewForDeal(Long dealId) {
        return reviewRepository.existsByDeal_DealId(dealId);
    }

    public ReviewResponse getReviewDetailForBuyer(Long reviewId, UUID buyerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));

        if (!review.getBuyer().getUserId().equals(buyerId)) {
            throw new AccessDeniedException("본인이 작성한 리뷰가 아닙니다.");
        }

        // Buyer 입장에서 seller에 대해 작성한 리뷰
        return ReviewResponse.from(review);
    }

    public ReviewResponse getReviewDetailForSeller(Long reviewId, UUID sellerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));

        if (!review.getSeller().getUserId().equals(sellerId)) {
            throw new AccessDeniedException("본인이 받은 리뷰가 아닙니다.");
        }

        // Seller 입장에서 buyer가 작성한 리뷰
        return ReviewResponse.from(review);
    }

}