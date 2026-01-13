package kr.eolmago.repository.favorite.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.favorite.response.FavoriteAuctionDto;
import kr.eolmago.repository.favorite.FavoriteRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static kr.eolmago.domain.entity.auction.QAuction.auction;
import static kr.eolmago.domain.entity.auction.QAuctionImage.auctionImage;
import static kr.eolmago.domain.entity.auction.QAuctionItem.auctionItem;
import static kr.eolmago.domain.entity.auction.QFavorite.favorite;
import static kr.eolmago.domain.entity.user.QUser.user;
import static kr.eolmago.domain.entity.user.QUserProfile.userProfile;

@Repository
@RequiredArgsConstructor
public class FavoriteRepositoryImpl implements FavoriteRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FavoriteAuctionDto> searchMyFavorites(Pageable pageable, UUID userId, String filter, String sort) {
        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(sort);

        List<FavoriteAuctionDto> content = queryFactory
                .select(Projections.constructor(
                        FavoriteAuctionDto.class,
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
                        auction.status,
                        favorite.createdAt // 찜한 시각
                ))
                .from(favorite)
                .innerJoin(favorite.auction, auction)
                .innerJoin(auction.auctionItem, auctionItem)
                .innerJoin(auctionImage)
                .on(auctionImage.auctionItem.eq(auctionItem)
                        .and(auctionImage.displayOrder.eq(0))) // 썸네일
                .innerJoin(auction.seller, user)
                .innerJoin(userProfile).on(userProfile.user.eq(user))
                .where(
                        favorite.user.userId.eq(userId),
                        statusFilter(filter)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers)
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(favorite.count())
                .from(favorite)
                .innerJoin(favorite.auction, auction)
                .where(
                        favorite.user.userId.eq(userId),
                        statusFilter(filter)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression statusFilter(String filter) {
        if (filter == null || filter.isBlank() || filter.equalsIgnoreCase("ALL")) {
            return null;
        }
        if (filter.equalsIgnoreCase("LIVE")) {
            return auction.status.eq(AuctionStatus.LIVE);
        }
        if (filter.equalsIgnoreCase("ENDED")) {
            // 스펙에 ENDED_SOLD를 쓰고 있으니 그 값을 사용 (프로젝트 enum에 맞춰 조정)
            return auction.status.eq(AuctionStatus.ENDED_SOLD);
        }
        return null;
    }

    private OrderSpecifier<?>[] createOrderSpecifiers(String sort) {
        if (sort == null || sort.isBlank()) sort = "recent";

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        switch (sort.toLowerCase()) {
            case "recent" -> orders.add(favorite.createdAt.desc());
            case "deadline" -> orders.add(auction.endAt.asc());
            case "price_asc" -> orders.add(auction.currentPrice.asc());
            case "price_desc" -> orders.add(auction.currentPrice.desc());
            default -> orders.add(favorite.createdAt.desc());
        }

        // tie-breaker
        orders.add(auction.auctionId.desc());

        return orders.toArray(new OrderSpecifier[0]);
    }
}
