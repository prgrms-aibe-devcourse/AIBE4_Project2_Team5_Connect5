package kr.eolmago.repository.deal.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.user.QSocialLogin;
import kr.eolmago.domain.entity.user.QUser;
import kr.eolmago.domain.entity.user.QUserProfile;
import kr.eolmago.dto.api.deal.response.DealDetailDto;
import kr.eolmago.dto.api.deal.response.DealPdfDto;
import kr.eolmago.repository.deal.DealRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static kr.eolmago.domain.entity.auction.QAuction.auction;
import static kr.eolmago.domain.entity.auction.QAuctionImage.auctionImage;
import static kr.eolmago.domain.entity.auction.QAuctionItem.auctionItem;
import static kr.eolmago.domain.entity.deal.QDeal.deal;
import static kr.eolmago.domain.entity.user.QSocialLogin.socialLogin;
import static kr.eolmago.domain.entity.user.QUser.user;
import static kr.eolmago.domain.entity.user.QUserProfile.userProfile;

/**
 * Deal Custom Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class DealRepositoryImpl implements DealRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<DealDetailDto> findDetailById(Long dealId) {
        var seller = user;
        var buyer = new QUser("buyer");
        var sellerProfile = userProfile;
        var buyerProfile = new QUserProfile("buyerProfile");

        DealDetailDto result = queryFactory
                .select(Projections.constructor(
                        DealDetailDto.class,
                        deal.dealId,
                        deal.finalPrice,
                        deal.status.stringValue(),
                        deal.createdAt.stringValue(),
                        deal.sellerConfirmedAt.isNotNull(),
                        deal.buyerConfirmedAt.isNotNull(),
                        deal.sellerConfirmedAt.stringValue(),
                        deal.buyerConfirmedAt.stringValue(),
                        deal.confirmedAt.stringValue(),
                        deal.confirmByAt.stringValue(),
                        deal.shipByAt.stringValue(),
                        deal.completedAt.stringValue(),
                        auction.auctionId,
                        auction.title,
                        auctionItem.itemName,
                        auctionItem.category,
                        auctionItem.condition,
                        auctionItem.specs,
                        deal.seller.userId,
                        deal.buyer.userId,
                        sellerProfile.nickname,
                        buyerProfile.nickname,
                        auctionImage.imageUrl                               // 썸네일 이미지 URL
                ))
                .from(deal)
                .innerJoin(deal.auction, auction)
                .innerJoin(auction.auctionItem, auctionItem)
                .innerJoin(deal.seller, seller)
                .innerJoin(deal.buyer, buyer)
                .innerJoin(sellerProfile).on(sellerProfile.user.eq(seller))
                .innerJoin(buyerProfile).on(buyerProfile.user.eq(buyer))
                .leftJoin(auctionImage).on(auctionImage.auctionItem.eq(auctionItem)
                        .and(auctionImage.displayOrder.eq(0)))
                .where(deal.dealId.eq(dealId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * PDF 생성을 위한 Deal 정보 조회
     *
     * Deal + Auction + AuctionItem + User + UserProfile + SocialLogin을 조인하여
     * 한 번의 쿼리로 필요한 모든 데이터 조회
     */
    @Override
    public Optional<DealPdfDto> findPdfDataByDealId(Long dealId) {
        // seller와 buyer를 구분하기 위해 별칭 생성
        var seller = user;
        var buyer = new QUser("buyer");
        var sellerProfile = userProfile;
        var buyerProfile = new QUserProfile("buyerProfile");
        var sellerLogin = socialLogin;
        var buyerLogin = new QSocialLogin("buyerLogin");

        DealPdfDto result = queryFactory
                .select(Projections.constructor(
                        DealPdfDto.class,
                        deal.dealId,
                        auction.auctionId,
                        auction.title,
                        auctionItem.itemName,
                        seller.userId,                                  // 판매자 ID
                        sellerLogin.email,
                        sellerProfile.phoneNumber,
                        buyer.userId,                                   // 구매자 ID
                        buyerLogin.email,
                        buyerProfile.phoneNumber,
                        deal.finalPrice,
                        deal.status,
                        deal.completedAt,
                        deal.shippingNumber,
                        deal.shippingCarrierCode,
                        deal.createdAt
                ))
                .from(deal)
                .innerJoin(deal.auction, auction)
                .innerJoin(auction.auctionItem, auctionItem)
                .innerJoin(deal.seller, seller)
                .innerJoin(deal.buyer, buyer)
                .innerJoin(sellerProfile).on(sellerProfile.user.eq(seller))
                .innerJoin(buyerProfile).on(buyerProfile.user.eq(buyer))
                .innerJoin(sellerLogin).on(sellerLogin.user.eq(seller))
                .innerJoin(buyerLogin).on(buyerLogin.user.eq(buyer))
                .where(deal.dealId.eq(dealId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}