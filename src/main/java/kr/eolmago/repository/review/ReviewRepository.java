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

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByDealAndBuyer(Deal deal, User buyer);

    List<Review> findBySeller_UserId(UUID sellerId);

    List<Review> findByBuyer_UserId(UUID buyerId);

    boolean existsByDeal_DealId(Long dealId);

    Optional<Review> findByDeal_DealId(Long dealId);

    Optional<Review> findByDeal_DealIdAndBuyer_UserId(Long dealId, UUID buyerId);

    Optional<Review> findByDeal_DealIdAndSeller_UserId(Long dealId, UUID sellerId);

    @Query("select r.deal.dealId from Review r where r.deal.dealId in :dealIds")
    List<Long> findReviewedDealIds(@Param("dealIds") List<Long> dealIds);

    @Query("select avg(r.rating) from Review r where r.seller.userId = :sellerId")
    Double findAverageRatingBySeller(@Param("sellerId") UUID sellerId);

    long countBySeller_UserId(UUID sellerId);

    /**
     * ✅ 받은 리뷰 목록 (sellerId 기준) - record DTO로 직접 조회
     */
    @Query(
            value = """
                select new kr.eolmago.dto.api.review.response.ReceivedReviewDto(
                    r.reviewId,
                    r.deal.dealId,
                    r.rating,
                    r.content,
                    r.createdAt,
                    b.userId,
                    bp.nickname,
                    bp.profileImageUrl
                )
                from Review r
                join r.buyer b
                join b.userProfile bp
                where r.seller.userId = :sellerId
                order by r.createdAt desc
            """,
            countQuery = """
                select count(r.reviewId)
                from Review r
                where r.seller.userId = :sellerId
            """
    )
    Page<ReceivedReviewDto> findReceivedReviewsBySellerId(
            @Param("sellerId") UUID sellerId,
            Pageable pageable
    );
}
