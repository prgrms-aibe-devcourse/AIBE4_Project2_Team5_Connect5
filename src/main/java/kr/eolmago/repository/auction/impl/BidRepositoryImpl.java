package kr.eolmago.repository.auction.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.repository.auction.BidRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static kr.eolmago.domain.entity.auction.QAuction.auction;
import static kr.eolmago.domain.entity.auction.QBid.bid;
import static kr.eolmago.domain.entity.user.QUser.user;

@Repository
@RequiredArgsConstructor
public class BidRepositoryImpl implements BidRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Bid> findByClientRequestIdAndBidderId(String clientRequestId, UUID bidderId) {
        Bid result = queryFactory
                .selectFrom(bid)
                .join(bid.auction, auction).fetchJoin()
                .where(
                        bid.clientRequestId.eq(clientRequestId),
                        bid.bidder.userId.eq(bidderId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Bid> findTopByAuctionOrderByAmountDescCreatedAtAsc(Auction auction) {
        Bid result = queryFactory
                .selectFrom(bid)
                .join(bid.bidder, user).fetchJoin()
                .where(bid.auction.eq(auction))
                .orderBy(
                        bid.amount.desc(),
                        bid.createdAt.asc()
                )
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<UUID> findTopBidderIdByAuction(Auction auction) {
        UUID result = queryFactory
                .select(bid.bidder.userId)
                .from(bid)
                .where(bid.auction.eq(auction))
                .orderBy(
                        bid.amount.desc(),
                        bid.createdAt.asc()
                )
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public boolean existsByAuction(Auction auction) {
        Integer count = queryFactory
                .selectOne()
                .from(bid)
                .where(bid.auction.eq(auction))
                .fetchFirst();

        return count != null;
    }
}
