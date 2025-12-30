package kr.eolmago.domain.entity.auction;

import kr.eolmago.domain.entity.common.CreatedAtEntity;
import kr.eolmago.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bids",
        indexes = {
                @Index(name = "idx_bids_auction_created", columnList = "auction_id,created_at"),
                @Index(name = "idx_bids_bidder", columnList = "bidder_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long bidId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false)
    private int amount;

    public static Bid create(
            Auction auction,
            User bidder,
            int amount
    ) {
        Bid bid = new Bid();
        bid.auction = auction;
        bid.bidder = bidder;
        bid.amount = amount;
        return bid;
    }
}