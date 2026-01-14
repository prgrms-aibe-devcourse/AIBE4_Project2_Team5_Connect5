package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.auction.response.BidCreateResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.global.util.DurationCalculator;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.auction.BidRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.auction.event.AuctionEndAtChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static kr.eolmago.service.auction.constants.AuctionConstants.*;

@Service
@RequiredArgsConstructor
public class BidCommandService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BidCreateResponse createBid(UUID auctionId, UUID buyerId, int amount, String requestId) {

        // 이미 처리된 요청이면 재처리 방지
        Optional<Bid> existing = bidRepository.findByClientRequestIdAndBidderId(requestId, buyerId);
        if (existing.isPresent()) {
            Bid bid = existing.get();
            if (bid.getAmount() != amount) {
                throw new BusinessException(ErrorCode.BID_IDEMPOTENCY_CONFLICT);
            }
            return buildBidCreateResponse(bid, false);
        }

        // FOR UPDATE DB 락 사용
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_LIVE);
        }

        if (auction.getSeller().getUserId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.SELLER_CANNOT_BID);
        }

        // 최소 입찰가, 최대 입찰가 검증
        int currentHighest = auction.getCurrentPrice();
        int increment = auction.getBidIncrement();

        int minAcceptable = currentHighest + increment;
        if (amount < minAcceptable) throw new BusinessException(ErrorCode.BID_INVALID_AMOUNT);
        if (amount > MAX_BID_AMOUNT) throw new BusinessException(ErrorCode.BID_AMOUNT_EXCEEDS_LIMIT);

        // 입찰 단위 검증, 입찰은 단위의 배수만 가능
        int diff = amount - currentHighest;
        if (increment > 0 && diff % increment != 0) throw new BusinessException(ErrorCode.BID_INVALID_INCREMENT);

        // 입찰 생성
        User bidder = userRepository.getReferenceById(buyerId);
        Bid bid = Bid.create(auction, bidder, amount, requestId);
        bidRepository.save(bid);

        // 경매 갱신
        auction.updateBid(amount);

        // 자동 연장
        OffsetDateTime now = OffsetDateTime.now();
        boolean extensionApplied = tryAutoExtension(auction, now);

        if (extensionApplied) {
            eventPublisher.publishEvent(new AuctionEndAtChangedEvent(auction.getAuctionId(), auction.getEndAt()));
        }

        return buildBidCreateResponse(bid, extensionApplied);
    }

    // 자동 연장
    private boolean tryAutoExtension(Auction auction, OffsetDateTime now) {
        OffsetDateTime endAt = auction.getEndAt();
        OffsetDateTime originalEndAt = auction.getOriginalEndAt();

        if (endAt == null || originalEndAt == null) {
            return false;
        }

        // 남은시간 계산
        long remainingSeconds = ChronoUnit.SECONDS.between(now, endAt);
        if (remainingSeconds <= 0) {
            return false;
        }

        if (remainingSeconds > EXTENSION_THRESHOLD_SECONDS) {
            return false;
        }

        OffsetDateTime candidateEndAt = endAt.plusSeconds(EXTENSION_DURATION_SECONDS);
        OffsetDateTime capEndAt = now.plusSeconds(MAX_REMAINING_SECONDS);
        OffsetDateTime hardCapEndAt = originalEndAt.plusHours(HARD_MAX_EXTENSION_HOURS);

        // 최종 종료시각 갱신
        // 5분 이하 남았을 때 5분 자동 연장, 연장 후 남은 시간은 최대 30분을 넘지 않도록 캡 적용
        // 원래 종료 시간으로부터 최대 12시간까지만 연장 가능
        OffsetDateTime newEndAt = candidateEndAt;
        if (newEndAt.isAfter(capEndAt)) {
            newEndAt = capEndAt;
        }
        if (newEndAt.isAfter(hardCapEndAt)) {
            newEndAt = hardCapEndAt;
        }

        if (!newEndAt.isAfter(endAt)) {
            return false;
        }

        int newDurationHours = DurationCalculator.calculateHoursBetween(originalEndAt, newEndAt);
        auction.extendEndTime(newEndAt, newDurationHours);

        return true;
    }

    private BidCreateResponse buildBidCreateResponse(Bid bid, boolean extensionApplied) {
        Auction auction = bid.getAuction();

        int currentHighest = auction.getCurrentPrice();
        int minAcceptable = currentHighest + auction.getBidIncrement();

        // 현재 최고 입찰자 ID 조회
        UUID highestBidderId = bidRepository.findTopBidderIdByAuction(auction).orElse(null);

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