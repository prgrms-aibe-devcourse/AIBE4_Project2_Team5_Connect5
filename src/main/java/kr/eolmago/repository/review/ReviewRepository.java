package kr.eolmago.repository.review;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.review.Review;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.review.response.ReceivedReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    List<Review> findBySeller_UserId(UUID sellerId);

    List<Review> findByBuyer_UserId(UUID buyerId);

    boolean existsByDeal_DealId(Long dealId);

    Optional<Review> findByDeal_DealId(Long dealId);

    Optional<Review> findByDeal_DealIdAndBuyer_UserId(Long dealId, UUID buyerId);

    Optional<Review> findByDeal_DealIdAndSeller_UserId(Long dealId, UUID sellerId);
}