package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.dto.api.deal.request.CreateDealFromAuctionRequest;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.service.auction.event.AuctionSoldEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSoldDealListener {

	private final AuctionRepository auctionRepository;
	private final DealCreationService dealCreationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onAuctionSold(AuctionSoldEvent event) {
		if (event == null || event.auctionId() == null) return;

		try {
			Auction auction = auctionRepository.findById(event.auctionId())
				.orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

			Long finalPrice = (long) auction.getCurrentPrice();

			CreateDealFromAuctionRequest req = new CreateDealFromAuctionRequest(
				event.auctionId(),
				event.sellerId(),
				event.buyerId(),
				finalPrice
			);

			dealCreationService.createDealFromAuction(req);

			log.info("[AUC_SOLD_DEAL_CREATE_OK] auctionId={}, sellerId={}, buyerId={}, finalPrice={}",
				event.auctionId(), event.sellerId(), event.buyerId(), finalPrice);

		} catch (BusinessException e) {
			if (e.getErrorCode() == ErrorCode.DEAL_ALREADY_EXISTS) {
				log.info("[AUC_SOLD_DEAL_ALREADY_EXISTS] auctionId={}", event.auctionId());
				return;
			}
			throw e;
		}
	}
}
