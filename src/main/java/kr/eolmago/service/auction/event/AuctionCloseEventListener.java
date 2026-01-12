package kr.eolmago.service.auction.event;

import kr.eolmago.service.auction.AuctionCloseScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuctionCloseEventListener {

    private final AuctionCloseScheduler auctionCloseScheduler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuctionEndAtChanged(AuctionEndAtChangedEvent event) {
        if (event == null || event.auctionId() == null || event.endAt() == null) {
            return;
        }
        auctionCloseScheduler.scheduleOrReschedule(event.auctionId(), event.endAt());
    }
}