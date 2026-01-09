package kr.eolmago.repository.auction.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import kr.eolmago.repository.auction.AuctionRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static kr.eolmago.domain.entity.auction.QAuction.auction;
import static kr.eolmago.domain.entity.auction.QAuctionItem.auctionItem;
import static kr.eolmago.domain.entity.auction.QAuctionImage.auctionImage;
import static kr.eolmago.domain.entity.user.QUser.user;
import static kr.eolmago.domain.entity.user.QUserProfile.userProfile;

@Repository
@RequiredArgsConstructor
public class AuctionRepositoryImpl implements AuctionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AuctionListDto> searchList(Pageable pageable, String sortKey, AuctionStatus status, UUID sellerId) {
        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(sortKey);

        List<AuctionListDto> content = queryFactory
                .select(Projections.constructor(
                        AuctionListDto.class,
                        auction.auctionId,
                        auctionItem.auctionItemId,
                        auctionItem.itemName,
                        auction.title,
                        auctionImage.imageUrl,
                        userProfile.nickname,
                        auction.startPrice,
                        auction.currentPrice,
                        auction.finalPrice,
                        auction.bidCount,
                        auction.favoriteCount,
                        auction.endAt,
                        auction.status
                ))
                .from(auction)
                .innerJoin(auction.auctionItem, auctionItem)
                .innerJoin(auctionImage)
                .on(auctionImage.auctionItem.eq(auctionItem)
                        .and(auctionImage.displayOrder.eq(0)))
                .innerJoin(auction.seller, user)
                .innerJoin(userProfile)
                .on(userProfile.user.eq(user))
                .where(
                        status == null ? null : auction.status.eq(status),
                        sellerId == null ? null : user.userId.eq(sellerId)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers)
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(auction.count())
                .from(auction)
                .innerJoin(auction.auctionItem, auctionItem)
                .innerJoin(auctionImage)
                .on(auctionImage.auctionItem.eq(auctionItem)
                        .and(auctionImage.displayOrder.eq(0)))
                .innerJoin(auction.seller, user)
                .innerJoin(userProfile)
                .on(userProfile.user.eq(user))
                .where(
                        status == null ? null : auction.status.eq(status),
                        sellerId == null ? null : user.userId.eq(sellerId)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private OrderSpecifier<?>[] createOrderSpecifiers(String sortKey) {
        if (sortKey == null || sortKey.isEmpty()) {
            sortKey = "latest";
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        switch (sortKey.toLowerCase()) {
            case "latest" -> orders.add(auction.createdAt.desc());
            case "deadline" -> orders.add(auction.endAt.asc());
            case "price_asc" -> orders.add(auction.currentPrice.asc());
            case "price_desc" -> orders.add(auction.currentPrice.desc());
            case "popular" -> {
                orders.add(auction.bidCount.desc());
                orders.add(auction.favoriteCount.desc());
            }
            default -> orders.add(auction.createdAt.desc());
        }

        orders.add(auction.auctionId.desc());

        return orders.toArray(new OrderSpecifier[0]);
    }
}
