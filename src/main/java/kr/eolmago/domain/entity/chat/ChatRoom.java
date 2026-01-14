package kr.eolmago.domain.entity.chat;

import java.util.UUID;
import kr.eolmago.domain.entity.common.AuditableEntity;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "chat_rooms",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_chat_rooms_auction_type",
            columnNames = {"auction_id", "room_type"}
        ),
        @UniqueConstraint(
            name = "uk_chat_rooms_notification",
            columnNames = {"room_type", "target_user_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private ChatRoomType roomType;

    // NOTIFICATION 방에서만 사용 (유저당 1개)
    @Column(name = "target_user_id")
    private UUID targetUserId;

    // AUCTION 방에서만 사용 (NOTIFICATION은 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
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

    public static ChatRoom createAuctionRoom(Auction auction, User seller, User buyer) {
        ChatRoom room = new ChatRoom();
        room.roomType = ChatRoomType.AUCTION;
        room.targetUserId = null;
        room.auction = auction;
        room.seller = seller;
        room.buyer = buyer;
        return room;
    }

    public static ChatRoom createNotificationRoom(User botUser, User targetUser) {
        ChatRoom room = new ChatRoom();
        room.roomType = ChatRoomType.NOTIFICATION;
        room.targetUserId = targetUser.getUserId();
        room.auction = null;
        room.seller = botUser; // 봇이 sender가 되게 seller로 둠
        room.buyer = targetUser; // 유저는 buyer
        return room;
    }

    public void updateLastMessageId(Long messageId) {
        this.lastMessageId = messageId;
    }
}
