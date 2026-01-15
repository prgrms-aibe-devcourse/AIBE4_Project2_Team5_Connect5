package kr.eolmago.repository.review.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.review.QReview;
import kr.eolmago.repository.review.ReviewRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findReviewedDealIds(List<Long> dealIds) {
        QReview r = QReview.review;
        return queryFactory
                .select(r.deal.dealId)
                .from(r)
                .where(r.deal.dealId.in(dealIds))
                .distinct()
                .fetch();
    }

    @Override
    public Double findAverageRatingBySeller(UUID sellerId) {
        QReview r = QReview.review;
        return queryFactory
                .select(r.rating.avg())
                .from(r)
                .where(r.seller.userId.eq(sellerId))
                .fetchOne();
    }
}