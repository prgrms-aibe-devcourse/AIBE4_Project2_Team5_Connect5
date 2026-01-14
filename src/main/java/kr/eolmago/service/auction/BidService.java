package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.auction.request.BidCreateRequest;
import kr.eolmago.dto.api.auction.response.BidCreateResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.global.util.DurationCalculator;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.auction.BidRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.auction.event.AuctionEndAtChangedEvent;
import kr.eolmago.service.notification.publish.NotificationPublishCommand;
import kr.eolmago.service.notification.publish.NotificationPublisher;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static kr.eolmago.global.constants.AuctionConstants.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPublisher notificationPublisher;

    // 입찰 생성
    @Transactional
    public BidCreateResponse createBid(UUID auctionId, BidCreateRequest request, UUID buyer_id) {

        // 입찰 중복 처리 방지
        String requestId = request.clientRequestId();
        Optional<Bid> existing = bidRepository.findByClientRequestIdAndBidderId(requestId, buyer_id);
        if (existing.isPresent()) {
            Bid bid = existing.get();
            if (bid.getAmount() == request.amount()) {
                // 동일 요청 재시도 -> 기존 결과 반환
                return buildBidCreateResponse(bid, false);
            }
            throw new BusinessException(ErrorCode.BID_IDEMPOTENCY_CONFLICT);
        }

        // 예외 처리
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_LIVE);
        }

        if (auction.getSeller().getUserId().equals(buyer_id)) {
            throw new BusinessException(ErrorCode.SELLER_CANNOT_BID);
        }

        // 최소 입찰가, 최대 입찰가 검증
        int currentHighest = auction.getCurrentPrice();
        int increment = auction.getBidIncrement();
        int amount = request.amount();

        int minAcceptable = currentHighest + increment;
        if (amount < minAcceptable) throw new BusinessException(ErrorCode.BID_INVALID_AMOUNT);
        if (amount > MAX_BID_AMOUNT) throw new BusinessException(ErrorCode.BID_AMOUNT_EXCEEDS_LIMIT);

        // 입찰 단위 검증, 입찰은 단위의 배수만 가능
        int diff = amount - currentHighest;
        if (increment > 0 && diff % increment != 0) throw new BusinessException(ErrorCode.BID_INVALID_INCREMENT);

        UUID prevHighestBidderId = bidRepository.findTopBidderIdByAuction(auction).orElse(null);

        // 입찰 생성
        User bidder = userRepository.getReferenceById(buyer_id);
        Bid bid = Bid.create(auction, bidder, amount, requestId);
        bidRepository.save(bid);

        // 경매 최고가 갱신, 입찰 카운트 증가
        auction.updateBid(amount);

        OffsetDateTime now = OffsetDateTime.now();
        boolean extensionApplied = tryAutoExtension(auction, now);

        notificationPublisher.publish(
            NotificationPublishCommand.bidAccepted(buyer_id, auctionId, amount)
        );

        if (prevHighestBidderId != null && !prevHighestBidderId.equals(buyer_id)) {
            notificationPublisher.publish(
                NotificationPublishCommand.bidOutbid(prevHighestBidderId, auctionId)
            );
        }

        return buildBidCreateResponse(bid, extensionApplied);
    }

    // 자동 연장
    private boolean tryAutoExtension(Auction auction, OffsetDateTime now) {
        OffsetDateTime endAt = auction.getEndAt();
        OffsetDateTime originalEndAt = auction.getOriginalEndAt();

        if (endAt == null || originalEndAt == null) return false;

        long remainingSeconds = ChronoUnit.SECONDS.between(now, endAt);
        if (remainingSeconds <= 0) return false;
        if (remainingSeconds > EXTENSION_THRESHOLD_SECONDS) return false;

        OffsetDateTime candidateEndAt = endAt.plusSeconds(EXTENSION_DURATION_SECONDS);
        OffsetDateTime capEndAt = now.plusSeconds(MAX_REMAINING_SECONDS);
        OffsetDateTime hardCapEndAt = originalEndAt.plusHours(HARD_MAX_EXTENSION_HOURS);

        OffsetDateTime newEndAt = candidateEndAt;
        if (newEndAt.isAfter(capEndAt)) newEndAt = capEndAt;
        if (newEndAt.isAfter(hardCapEndAt)) newEndAt = hardCapEndAt;

        if (!newEndAt.isAfter(endAt)) return false;

        int newDurationHours = DurationCalculator.calculateHoursBetween(originalEndAt, newEndAt);
        auction.extendEndTime(newEndAt, newDurationHours);

        eventPublisher.publishEvent(new AuctionEndAtChangedEvent(auction.getAuctionId(), newEndAt));
        return true;
    }


    private BidCreateResponse buildBidCreateResponse(Bid bid, boolean extensionApplied) {
        Auction auction = bid.getAuction();
        int currentHighest = auction.getCurrentPrice();
        int minAcceptable = currentHighest + auction.getBidIncrement();

        // 현재 최고 입찰자 ID 조회
        UUID highestBidderId = bidRepository.findTopBidderIdByAuction(auction)
                .orElse(null);

        return new BidCreateResponse(
                bid.getBidId(),
                auction.getAuctionId(),
                bid.getAmount(),
                currentHighest,
                minAcceptable,
                auction.getEndAt(),
                extensionApplied,
                highestBidderId
        );
    }
}