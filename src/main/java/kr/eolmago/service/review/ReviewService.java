package kr.eolmago.service.review;

import jakarta.persistence.EntityNotFoundException;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.domain.entity.review.Review;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserProfile;
import kr.eolmago.dto.api.review.request.ReviewCreateRequest;
import kr.eolmago.dto.api.review.response.ReviewResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.review.ReviewRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void createReview(Long dealId, UUID reviewerId, ReviewCreateRequest request) {
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

        Review review = Review.create(deal, seller, buyer, request.rating(), request.content());
        reviewRepository.save(review);
    }

    public List<ReviewResponse> getReviewsByBuyer(UUID buyerId) {
        return reviewRepository.findByBuyer_UserId(buyerId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    public List<ReviewResponse> getReviewsBySeller(UUID sellerId) {
        return reviewRepository.findBySeller_UserId(sellerId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    public ReviewResponse getReviewDetailForBuyer(Long reviewId, UUID buyerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));

        if (!review.getBuyer().getUserId().equals(buyerId)) {
            throw new AccessDeniedException("본인이 작성한 리뷰가 아닙니다.");
        }

        return toReviewResponse(review);
    }

    public ReviewResponse getReviewDetailForSeller(Long reviewId, UUID sellerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));

        if (!review.getSeller().getUserId().equals(sellerId)) {
            throw new AccessDeniedException("본인이 받은 리뷰가 아닙니다.");
        }

        return toReviewResponse(review);
    }

    public ReviewResponse getReviewByDealIdForBuyer(Long dealId, UUID buyerId) {
        Review review = reviewRepository.findByDeal_DealIdAndBuyer_UserId(dealId, buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
        return toReviewResponse(review);
    }

    public ReviewResponse getReviewByDealIdForSeller(Long dealId, UUID sellerId) {
        Review review = reviewRepository.findByDeal_DealIdAndSeller_UserId(dealId, sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
        return toReviewResponse(review);
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

    private ReviewResponse toReviewResponse(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getDeal().getDealId(),
                review.getDeal().getAuction().getTitle(),
                review.getDeal().getFinalPrice(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                extractNickname(review.getBuyer()),
                extractProfileImageUrl(review.getBuyer()),
                extractNickname(review.getSeller())
        );
    }

    private String extractNickname(User user) {
        if (user == null || user.getUserProfile() == null) return "-";
        String nick = user.getUserProfile().getNickname();
        return (nick == null || nick.isBlank()) ? "-" : nick;
    }

    private String extractProfileImageUrl(User user) {
        if (user == null || user.getUserProfile() == null) return null;
        UserProfile profile = user.getUserProfile();
        return profile.getProfileImageUrl();
    }
}
