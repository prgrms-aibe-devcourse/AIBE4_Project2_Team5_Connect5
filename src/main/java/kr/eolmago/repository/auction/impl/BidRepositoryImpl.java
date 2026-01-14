package kr.eolmago.repository.auction.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.dto.api.auction.response.BidHistoryRow;
import kr.eolmago.dto.api.auction.response.BidderFirstBidDto;
import kr.eolmago.repository.auction.BidRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public Page<BidHistoryRow> findBidHistory(UUID auctionId, Pageable pageable) {

        List<BidHistoryRow> content = queryFactory
                .select(Projections.constructor(
                        BidHistoryRow.class,
                        bid.bidId,
                        bid.createdAt,
                        bid.amount,
                        bid.bidder.userId
                ))
                .from(bid)
                .where(bid.auction.auctionId.eq(auctionId))
                .orderBy(
                        bid.createdAt.desc(),
                        bid.bidId.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(bid.count())
                .from(bid)
                .where(bid.auction.auctionId.eq(auctionId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<BidderFirstBidDto> findBidderOrder(UUID auctionId) {
        return queryFactory
                .select(Projections.constructor(
                        BidderFirstBidDto.class,
                        bid.bidder.userId,
                        bid.createdAt.min()
                ))
                .from(bid)
                .where(bid.auction.auctionId.eq(auctionId))
                .groupBy(bid.bidder.userId)
                .orderBy(bid.createdAt.min().asc())
                .fetch();
    }
}
