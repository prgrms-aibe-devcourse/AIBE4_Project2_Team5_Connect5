package kr.eolmago.repository.deal.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.domain.entity.report.enums.ReportStatus;
import kr.eolmago.domain.entity.user.QSocialLogin;
import kr.eolmago.domain.entity.user.QUser;
import kr.eolmago.domain.entity.user.QUserProfile;
import kr.eolmago.dto.api.deal.response.DealDetailDto;
import kr.eolmago.dto.api.deal.response.DealPdfDto;
import kr.eolmago.repository.deal.DealRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kr.eolmago.domain.entity.auction.QAuction.auction;
import static kr.eolmago.domain.entity.auction.QAuctionImage.auctionImage;
import static kr.eolmago.domain.entity.auction.QAuctionItem.auctionItem;
import static kr.eolmago.domain.entity.deal.QDeal.deal;
import static kr.eolmago.domain.entity.report.QReport.report;
import static kr.eolmago.domain.entity.user.QSocialLogin.socialLogin;
import static kr.eolmago.domain.entity.user.QUser.user;
import static kr.eolmago.domain.entity.user.QUserProfile.userProfile;

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

    /**
     * 자동 완료 가능한 거래 조회
     *
     * 조건:
     * 1. CONFIRMED 상태
     * 2. shippedAt이 threshold 이전
     * 3. 해당 auction에 진행 중인 신고가 없음 (PENDING, UNDER_REVIEW)
     */
    @Override
    public List<Deal> findCompletableDeals(OffsetDateTime shippedAtThreshold) {
        return queryFactory
                .selectFrom(deal)
                .where(
                        // 1. CONFIRMED 상태
                        deal.status.eq(DealStatus.CONFIRMED),

                        // 2. shippedAt이 threshold 이전 (배송 후 N일 경과)
                        deal.shippedAt.lt(shippedAtThreshold),

                        // 3. 진행 중인 신고가 없음
                        queryFactory.selectOne()
                                .from(report)
                                .where(
                                        report.auction.eq(deal.auction),
                                        report.status.in(ReportStatus.PENDING, ReportStatus.UNDER_REVIEW)
                                )
                                .notExists()
                )
                .fetch();
    }

    @Override
    public long countBySellerIdAndStatus(UUID sellerId, DealStatus status) {
        Long cnt = queryFactory
                .select(deal.dealId.count())
                .from(deal)
                .where(
                        deal.seller.userId.eq(sellerId),
                        deal.status.eq(status)
                )
                .fetchOne();

        return cnt != null ? cnt : 0L;
    }
}