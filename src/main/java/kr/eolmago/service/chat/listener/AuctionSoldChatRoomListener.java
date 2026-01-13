package kr.eolmago.service.chat.listener;

import kr.eolmago.service.auction.event.AuctionSoldEvent;
import kr.eolmago.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSoldChatRoomListener {

	private final ChatService chatService;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
	public void onAuctionSold(AuctionSoldEvent event) {
		log.info("[AUC_SOLD] listener received auctionId={}, sellerId={}, buyerId={}",
			event.auctionId(), event.sellerId(), event.buyerId());

		Long roomId = chatService.createOrGetRoom(event.auctionId(), event.sellerId());
		log.info("[AUC_SOLD] chat room created/ensured roomId={}", roomId);
	}
}
