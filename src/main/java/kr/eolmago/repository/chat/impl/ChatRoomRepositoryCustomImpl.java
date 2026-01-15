package kr.eolmago.repository.chat.impl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.eolmago.domain.entity.auction.QAuction;
import kr.eolmago.domain.entity.auction.QAuctionImage;
import kr.eolmago.domain.entity.auction.QAuctionItem;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;
import kr.eolmago.domain.entity.chat.QChatMessage;
import kr.eolmago.domain.entity.chat.QChatRoom;
import kr.eolmago.domain.entity.user.QUser;
import kr.eolmago.repository.chat.ChatRoomRepositoryCustom;
import kr.eolmago.repository.chat.ChatRoomSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class ChatRoomRepositoryCustomImpl implements ChatRoomRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private final EntityManager em;

	@Override
	@Transactional(readOnly = true)
	public List<ChatRoomSummaryProjection> findMyRoomSummariesByType(UUID userId, ChatRoomType roomType) {

		QChatRoom cr = QChatRoom.chatRoom;
		QAuction a = QAuction.auction;
		QAuctionItem it = QAuctionItem.auctionItem;

		QChatMessage msg = QChatMessage.chatMessage;
		QChatMessage msg2 = new QChatMessage("msg2");

		QAuctionImage img = QAuctionImage.auctionImage;
		QAuctionImage img2 = new QAuctionImage("img2");

		NumberExpression<Long> myLastReadId =
			new CaseBuilder()
				.when(cr.seller.userId.eq(userId)).then(cr.sellerLastReadId.coalesce(0L))
				.when(cr.buyer.userId.eq(userId)).then(cr.buyerLastReadId.coalesce(0L))
				.otherwise(0L);

		Expression<Long> lastMsgId =
			JPAExpressions.select(msg.chatMessageId.max())
				.from(msg)
				.where(msg.chatRoom.eq(cr));

		Expression<String> lastMessage =
			JPAExpressions.select(msg2.content)
				.from(msg2)
				.where(msg2.chatMessageId.eq(lastMsgId));

		Expression<OffsetDateTime> lastMessageAt =
			JPAExpressions.select(msg2.createdAt)
				.from(msg2)
				.where(msg2.chatMessageId.eq(lastMsgId));

		Expression<Long> unreadCount =
			JPAExpressions.select(msg.count())
				.from(msg)
				.where(
					msg.chatRoom.eq(cr),
					msg.chatMessageId.gt(myLastReadId),
					msg.sender.userId.ne(userId)
				);

		Expression<String> thumbnailUrl =
			JPAExpressions.select(img2.imageUrl)
				.from(img2)
				.where(
					img2.auctionItem.eq(it)
						.and(img2.displayOrder.eq(
							JPAExpressions.select(img.displayOrder.min())
								.from(img)
								.where(img.auctionItem.eq(it))
						))
				);

		return queryFactory
			.select(Projections.constructor(
				ChatRoomSummaryProjection.class,
				cr.chatRoomId,
				cr.roomType,
				a.auctionId,
				a.title,
				thumbnailUrl,
				lastMessage,
				lastMessageAt,
				Expressions.numberTemplate(Long.class, "coalesce({0},0)", unreadCount)
			))
			.from(cr)
			.leftJoin(cr.auction, a)
			.leftJoin(a.auctionItem, it)
			.where(
				cr.roomType.eq(roomType),
				cr.seller.userId.eq(userId)
					.or(cr.buyer.userId.eq(userId))
					.or(cr.targetUserId.eq(userId))
			)
			.orderBy(
				Expressions.numberTemplate(Long.class, "coalesce({0},0)", lastMsgId).desc(),
				cr.updatedAt.desc()
			)
			.fetch();
	}

	@Override
	@Transactional
	public int markRead(Long roomId, UUID userId, Long messageId) {

		QChatRoom cr = QChatRoom.chatRoom;

		Expression<Long> newSellerLastRead =
			new CaseBuilder()
				.when(cr.seller.userId.eq(userId))
				.then(Expressions.numberTemplate(
					Long.class,
					"greatest(coalesce({0},0), {1})",
					cr.sellerLastReadId,
					messageId
				))
				.otherwise(cr.sellerLastReadId);

		Expression<Long> newBuyerLastRead =
			new CaseBuilder()
				.when(cr.buyer.userId.eq(userId))
				.then(Expressions.numberTemplate(
					Long.class,
					"greatest(coalesce({0},0), {1})",
					cr.buyerLastReadId,
					messageId
				))
				.otherwise(cr.buyerLastReadId);

		long updated = queryFactory
			.update(cr)
			.set(cr.sellerLastReadId, newSellerLastRead)
			.set(cr.buyerLastReadId, newBuyerLastRead)
			.where(cr.chatRoomId.eq(roomId))
			.execute();

		em.flush();
		em.clear();
		return (int) updated;
	}

    @Override
    public Optional<ChatRoom> findRoomViewById(Long roomId) {
        QChatRoom r = QChatRoom.chatRoom;

        QUser s = new QUser("seller");
        QUser b = new QUser("buyer");
        QAuction a = QAuction.auction;

        ChatRoom result = queryFactory
                .selectFrom(r)
                .distinct()
                .join(r.seller, s).fetchJoin()
                .leftJoin(r.buyer, b).fetchJoin()
                .leftJoin(r.auction, a).fetchJoin()
                .where(r.chatRoomId.eq(roomId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
