package kr.eolmago.domain.entity.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionEndReason;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.common.CreatedAtEntity;
import kr.eolmago.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "auctions",
        indexes = {
                @Index(name = "idx_auctions_status_end_at", columnList = "status,end_at"),
                @Index(name = "idx_auctions_seller", columnList = "seller_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID auctionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_item_id", nullable = false)
    private AuctionItem auctionItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(nullable = false)
    private int startPrice;

    @Column(nullable = false)
    private int currentPrice;

    @Column(nullable = false)
    private int bidIncrement;

    @Column(nullable = false)
    private int bidCount;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private int favoriteCount;

    @Column
    private OffsetDateTime startAt;

    @Column
    private OffsetDateTime endAt;

    @Column(nullable = false)
    private int durationHours;

    @Column(nullable = false)
    private int extendCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuctionEndReason endReason;

    @Column
    private Long finalPrice;

    public static Auction create(
            AuctionItem auctionItem,
            User seller,
            String title,
            String description,
            AuctionStatus status,
            int startPrice,
            int bidIncrement,
            int durationHours,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        Auction auction = new Auction();
        auction.auctionItem = auctionItem;
        auction.seller = seller;
        auction.title = title;
        auction.description = description;
        auction.status = status;
        auction.startPrice = startPrice;
        auction.currentPrice = startPrice;
        auction.bidIncrement = bidIncrement;
        auction.bidCount = 0;
        auction.viewCount = 0;
        auction.favoriteCount = 0;
        auction.durationHours = durationHours;
        auction.startAt = startAt;
        auction.endAt = endAt;
        auction.extendCount = 0;

        return auction;
    }

}