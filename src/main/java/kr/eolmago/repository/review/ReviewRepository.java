package kr.eolmago.repository.review;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.review.Review;
import kr.eolmago.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 거래 + 구매자 기준으로 리뷰 존재 여부 확인 (중복 작성 방지)
    Optional<Review> findByDealAndBuyer(Deal deal, User buyer);

    // 내가 판매자인 거래에 대해 받은 리뷰들 (List)
    List<Review> findBySeller_UserId(UUID sellerId);

    // 내가 구매자인 거래에서 내가 작성한 리뷰들 (List)
    List<Review> findByBuyer_UserId(UUID buyerId);

    boolean existsByDeal_DealId(Long dealId);

    Optional<Review> findByDeal_DealId(Long dealId);

    Optional<Review> findByDeal_DealIdAndBuyer_UserId(Long dealId, UUID buyerId);

    Optional<Review> findByDeal_DealIdAndSeller_UserId(Long dealId, UUID sellerId);

    @Query("select r.deal.dealId from Review r where r.deal.dealId in :dealIds")
    List<Long> findReviewedDealIds(@Param("dealIds") List<Long> dealIds);

    // (선택) 판매자가 받은 리뷰 페이징 - 엔티티로 받는 기본 버전
    Page<Review> findBySeller_UserId(UUID sellerId, Pageable pageable);

    @Query("select avg(r.rating) from Review r where r.seller.userId = :sellerId")
    Double findAverageRatingBySeller(@Param("sellerId") UUID sellerId);

    long countBySeller_UserId(UUID sellerId);

    /**
     * ✅ 받은 리뷰 목록 API용 (추천)
     * - fetch join + pageable 대신 Projection으로 필요한 필드만 조회
     * - buyer.userProfile 정보 포함
     */
    @Query(
            value = """
            select
                r.reviewId as reviewId,
                r.deal.dealId as dealId,
                r.rating as rating,
                r.content as content,
                r.createdAt as createdAt,
                b.userId as buyerId,
                bp.nickname as buyerNickname,
                bp.profileImageUrl as buyerProfileImageUrl
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
    Page<ReceivedReviewProjection> findReceivedReviewsBySellerId(
            @Param("sellerId") UUID sellerId,
            Pageable pageable
    );
}
