package kr.eolmago.domain.entity.deal;

import kr.eolmago.domain.entity.common.AuditableEntity;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "deals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deals_auction", columnNames = {"auction_id"})
        },
        indexes = {
                @Index(name = "idx_deals_status_confirm_by", columnList = "status,confirm_by_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long dealId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false)
    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DealStatus status;

    @Column(nullable = false)
    private OffsetDateTime confirmByAt;

    private OffsetDateTime sellerConfirmedAt;

    private OffsetDateTime buyerConfirmedAt;

    private OffsetDateTime confirmedAt;

    private OffsetDateTime expiredAt;

    private OffsetDateTime disputedAt;

    private OffsetDateTime terminatedAt;

    @Column(columnDefinition = "text")
    private String terminationReason;

    private OffsetDateTime completedAt;

    private OffsetDateTime shipByAt;

    private OffsetDateTime shippedAt;

    @Column(length = 50)
    private String shippingNumber;

    @Column(length = 50)
    private String shippingCarrierCode;

    public static Deal create(
            Auction auction,
            User seller,
            User buyer,
            Long finalPrice,
            OffsetDateTime confirmByAt
    ) {
        Deal deal = new Deal();
        deal.auction = auction;
        deal.seller = seller;
        deal.buyer = buyer;
        deal.finalPrice = finalPrice;
        deal.status = DealStatus.PENDING_CONFIRMATION; // 최초 상태는 확정 대기
        deal.confirmByAt = confirmByAt;
        return deal;
    }
}