package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.AuctionImage;
import kr.eolmago.domain.entity.auction.AuctionItem;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.auction.response.AuctionRepublishResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.*;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.auction.event.AuctionSoldEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuctionCloseService {

    private final AuctionRepository auctionRepository;
    private final AuctionCloseRepository auctionCloseRepository;
    private final BidRepository bidRepository;

    private final AuctionItemRepository auctionItemRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;

    // 트랜잭션 전파 - 항상 새로운 트랜잭션을 시작
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeAuction(UUID auctionId) {

        Auction auction = auctionCloseRepository.findByIdForUpdate(auctionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (auction.getEndAt() == null || auction.getEndAt().isAfter(now)) {
            return;
        }

        Bid highestBid = bidRepository
            .findTopByAuctionOrderByAmountDescCreatedAtAsc(auction)
            .orElse(null);

        if (highestBid == null) {
            auction.closeAsUnsold();
            return;
        }

        User buyer = highestBid.getBidder();
        Long finalPrice = (long) highestBid.getAmount();

        auction.closeAsSold(buyer, finalPrice);

        AuctionSoldEvent event = new AuctionSoldEvent(
            auction.getAuctionId(),
            auction.getSeller().getUserId(),
            buyer.getUserId()
        );

        log.info("[AUC_SOLD_PUBLISH] auctionId={}, sellerId={}, buyerId={}, finalPrice={}",
            event.auctionId(), event.sellerId(), event.buyerId(), finalPrice);

        eventPublisher.publishEvent(event);
    }


    // 유찰 경매 재등록
    @Transactional
    public AuctionRepublishResponse republishUnsoldAuction(UUID auctionId, UUID sellerId) {

        Auction originalAuction = auctionCloseRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (!originalAuction.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.AUCTION_UNAUTHORIZED);
        }

        if (originalAuction.getStatus() != AuctionStatus.ENDED_UNSOLD) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_UNSOLD);
        }

        AuctionItem originalItem = originalAuction.getAuctionItem();
        List<AuctionImage> originalImages =
                auctionImageRepository.findByAuctionItemOrderByDisplayOrder(originalItem);

        AuctionItem newItem = AuctionItem.create(
                originalItem.getItemName(),
                originalItem.getCategory(),
                originalItem.getCondition(),
                new HashMap<>(originalItem.getSpecs())
        );
        auctionItemRepository.save(newItem);

        List<AuctionImage> newImages = new ArrayList<>();
        for (AuctionImage originalImage : originalImages) {
            newImages.add(AuctionImage.create(
                    newItem,
                    originalImage.getImageUrl(),
                    originalImage.getDisplayOrder()
            ));
        }
        auctionImageRepository.saveAll(newImages);

        User seller = userRepository.getReferenceById(sellerId);

        Auction newAuction = Auction.create(
                newItem,
                seller,
                originalAuction.getTitle(),
                originalAuction.getDescription(),
                AuctionStatus.DRAFT,
                originalAuction.getStartPrice(),
                originalAuction.getBidIncrement(),
                originalAuction.getDurationHours(),
                null,
                null
        );
        auctionCloseRepository.save(newAuction);

        return new AuctionRepublishResponse(
                newAuction.getAuctionId(),
                originalAuction.getAuctionId()
        );
    }

    // 판매자 경매 취소
    public void cancelAuctionBySeller(UUID auctionId, UUID sellerId) {

        Auction auction = auctionCloseRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.AUCTION_UNAUTHORIZED);
        }

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_LIVE);
        }

        // 입찰이 0일 때만 판매 취소 가능
        if (auction.getBidCount() > 0) {
            throw new BusinessException(ErrorCode.AUCTION_HAS_ACTIVE_BIDS);
        }
        if (bidRepository.existsByAuction(auction)) {
            throw new BusinessException(ErrorCode.AUCTION_HAS_ACTIVE_BIDS);
        }

        auction.cancelBySeller();
    }
}