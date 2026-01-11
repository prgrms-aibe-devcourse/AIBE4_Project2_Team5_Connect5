package kr.eolmago.domain.entity.chat;

import kr.eolmago.domain.entity.common.AuditableEntity;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    private Long sellerLastReadId;

    private Long buyerLastReadId;

    private Long lastMessageId;

    public static ChatRoom create(
            Auction auction,
            User seller,
            User buyer
    ) {
        ChatRoom room = new ChatRoom();
        room.auction = auction;
        room.seller = seller;
        room.buyer = buyer;
        return room;
    }

    public void updateLastMessageId(Long messageId) {
        this.lastMessageId = messageId;
    }
}

