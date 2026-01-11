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
import java.util.UUID;

@Entity
@Table(name = "deals")
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
    /**
     * 판매자 확인 처리
     */
    public void confirmBySeller() {
        this.sellerConfirmedAt = OffsetDateTime.now();
        checkBothConfirmed();
    }

    /**
     * 구매자 확인 처리
     */
    public void confirmByBuyer() {
        this.buyerConfirmedAt = OffsetDateTime.now();
        checkBothConfirmed();
    }

    /**
     * 양쪽 모두 확인했는지 체크하고 상태 변경
     */
    private void checkBothConfirmed() {
        if (sellerConfirmedAt != null && buyerConfirmedAt != null) {
            this.confirmedAt = OffsetDateTime.now();
            this.status = DealStatus.CONFIRMED;
        }
    }

    /**
     * 거래 완료 처리
     */
    public void complete() {
        if (this.status != DealStatus.CONFIRMED) {
            throw new IllegalStateException("확정된 거래만 완료할 수 있습니다");
        }
        this.status = DealStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    /**
     * 거래 종료 처리 (취소)
     */
    public void terminate(String reason) {
        if (this.status == DealStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 거래는 종료할 수 없습니다");
        }
        this.status = DealStatus.TERMINATED;
        this.terminatedAt = OffsetDateTime.now();
        this.terminationReason = reason;
    }

    /**
     * 거래 만료 처리
     */
    public void expire() {
        this.status = DealStatus.EXPIRED;
        this.expiredAt = OffsetDateTime.now();
    }

    /**
     * 배송 시작
     */
    public void startShipping(String shippingNumber, String carrierCode) {
        if (this.status != DealStatus.CONFIRMED) {
            throw new IllegalStateException("확정된 거래만 배송할 수 있습니다");
        }
        this.shippedAt = OffsetDateTime.now();
        this.shippingNumber = shippingNumber;
        this.shippingCarrierCode = carrierCode;
    }

    // ========================================
    // 비즈니스 로직 메서드
    // ========================================

    /**
     * 특정 사용자가 거래 당사자인지 확인
     */
    public boolean isParticipant(UUID userId) {
        return this.seller.getUserId().equals(userId)
                || this.buyer.getUserId().equals(userId);
    }

    /**
     * 판매자인지 확인
     */
    public boolean isSeller(UUID userId) {
        return this.seller.getUserId().equals(userId);
    }

    /**
     * 구매자인지 확인
     */
    public boolean isBuyer(UUID userId) {
        return this.buyer.getUserId().equals(userId);
    }

    /**
     * 완료 가능 여부
     */
    public boolean canComplete() {
        return this.status == DealStatus.CONFIRMED;
    }

    /**
     * 종료 가능 여부
     */
    public boolean canTerminate() {
        return this.status != DealStatus.COMPLETED;
    }
}