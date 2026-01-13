package kr.eolmago.service.review;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.domain.entity.review.Review;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.review.ReviewRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DealRepository dealRepository;
    private final UserRepository userRepository;

    /**
     * 구매자가 완료된 거래에 대해 판매자에게 리뷰를 작성하는 메서드.
     */
    @Transactional
    public ReviewResponse createReview(
            Long dealId,
            UUID userId,
            int rating,
            String content
    ) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1에서 5 사이여야 합니다.");
        }

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        // 완료된 거래만 리뷰 작성 가능
        if (deal.getStatus() != DealStatus.COMPLETED) {
            throw new IllegalStateException("완료된 거래에만 리뷰를 작성할 수 있습니다.");
        }

        if (deal.getBuyer() == null || deal.getBuyer().getUserId() == null) {
            throw new IllegalStateException("구매자 정보가 없습니다.");
        }
        if (deal.getSeller() == null || deal.getSeller().getUserId() == null) {
            throw new IllegalStateException("판매자 정보가 없습니다.");
        }

        // 현재 로그인 유저가 이 거래의 구매자인지 검증
        if (!deal.getBuyer().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 거래의 구매자만 리뷰를 작성할 수 있습니다.");
        }

        // 동일 거래에 동일 구매자가 이미 리뷰 작성했는지 체크
        List<Review> existing = reviewRepository.findByDeal_DealId(dealId);
        boolean alreadyExists = existing.stream()
                .anyMatch(review -> review.getBuyer() != null
                        && userId.equals(review.getBuyer().getUserId()));

        if (alreadyExists) {
            throw new IllegalStateException("이미 이 거래에 대한 리뷰를 작성하였습니다.");
        }

        // User 엔티티 로드 (일관성 및 지연 로딩 방지)
        User buyer = userRepository.findByUserId(deal.getBuyer().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("구매자 회원 정보를 찾을 수 없습니다."));
        User seller = userRepository.findByUserId(deal.getSeller().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("판매자 회원 정보를 찾을 수 없습니다."));

        Review review = Review.create(
                deal,
                seller,
                buyer,
                (short) rating,
                content
        );

        Review saved = reviewRepository.save(review);
        return ReviewResponse.from(saved);
    }

    public ReviewResponse getReview(Long reviewId) {
        Review review = findReviewById(reviewId);
        return ReviewResponse.from(review);
    }

    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getReviewsBySeller(UUID sellerId) {
        List<Review> reviews = reviewRepository.findBySeller_UserId(sellerId);
        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getReviewsByBuyer(UUID buyerId) {
        List<Review> reviews = reviewRepository.findByBuyer_UserId(buyerId);
        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReview(Long reviewId, UUID userId) {
        Review review = findReviewById(reviewId);

        boolean isSeller = review.getSeller() != null
                && review.getSeller().getUserId() != null
                && review.getSeller().getUserId().equals(userId);

        boolean isBuyer = review.getBuyer() != null
                && review.getBuyer().getUserId() != null
                && review.getBuyer().getUserId().equals(userId);

        if (!isSeller && !isBuyer) {
            throw new IllegalArgumentException("리뷰를 삭제할 권한이 없습니다.");
        }

        reviewRepository.delete(review);
    }

    private Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
    }
}
