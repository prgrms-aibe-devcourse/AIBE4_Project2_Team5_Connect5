package kr.eolmago.service.auction;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.dto.api.auction.request.BidCreateRequest;
import kr.eolmago.dto.api.auction.response.BidCreateResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.BidRepository;
import kr.eolmago.service.auction.stream.BidProcessingResult;
import kr.eolmago.service.auction.stream.BidResultStore;
import kr.eolmago.service.auction.stream.BidStreamProperties;
import kr.eolmago.service.auction.stream.BidStreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static kr.eolmago.service.auction.constants.AuctionConstants.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;

    private final StringRedisTemplate redisTemplate;
    private final BidStreamProperties props;
    private final BidResultStore bidResultStore;

    @Transactional
    public BidCreateResponse createBid(UUID auctionId, BidCreateRequest request, UUID buyerId) {

        long idempotencyTtlMs = props.getIdempotencyTtlMs();
        long apiWaitTimeoutMs = props.getApiWaitTimeoutMs();
        int amount = request.amount();

        // 멱등성 키(clientRequestId) 필수
        String requestId = request.clientRequestId();
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(ErrorCode.BID_IDEMPOTENCY_REQUIRED);
        }

        String resultKey = BidStreamSupport.resultKey(buyerId, requestId);

        // Redis 결과가 있으면 즉시 반환
        BidProcessingResult cached = bidResultStore.get(resultKey);
        if (cached != null && !cached.isPending()) {
            return resolveOrThrow(cached);
        }

        // 이미 저장된 요청이면 바로 반환
        Optional<Bid> existing = bidRepository.findByClientRequestIdAndBidderId(requestId, buyerId);
        if (existing.isPresent()) {

            Bid bid = existing.get();
            if (bid.getAmount() != amount) {
                throw new BusinessException(ErrorCode.BID_IDEMPOTENCY_CONFLICT);
            }

            return buildBidCreateResponse(bid, false);
        }

        // 결과키 PENDING
        bidResultStore.putPendingIfAbsent(resultKey, Duration.ofMillis(idempotencyTtlMs));

        // publishKey NX로 설정, 최초 1회만 Stream 발행
        // 키가 없을 때만 set -> 중복 발행 방지
        String publishKey = BidStreamSupport.publishKey(buyerId, requestId);
        Boolean firstPublish = redisTemplate.opsForValue().setIfAbsent(
                publishKey,
                "1",
                Duration.ofMillis(idempotencyTtlMs)
        );

        if (Boolean.TRUE.equals(firstPublish)) {
            publishToStream(auctionId, buyerId, amount, requestId);
        }

        // 결과키 대기(폴링)
        long deadline = System.currentTimeMillis() + apiWaitTimeoutMs;

        while (System.currentTimeMillis() < deadline) {
            BidProcessingResult result = bidResultStore.get(resultKey);
            if (result != null && !result.isPending()) {
                return resolveOrThrow(result);
            }
            sleepSilently(RESULT_WAIT_POLL_MS);
        }

        // 타임아웃 발생 시 DB 멱등 조회로 결과 복구
        Optional<Bid> afterTimeout = bidRepository.findByClientRequestIdAndBidderId(requestId, buyerId);
        if (afterTimeout.isPresent()) {
            Bid bid = afterTimeout.get();
            if (bid.getAmount() != amount) {
                throw new BusinessException(ErrorCode.BID_IDEMPOTENCY_CONFLICT);
            }
            return buildBidCreateResponse(bid, false); // 복구 시 자동 연장 여부는 false
        }

        throw new BusinessException(ErrorCode.BID_QUEUE_TIMEOUT);
    }

    private void publishToStream(UUID auctionId, UUID buyerId, int amount, String requestId) {
        // BidStreamProperties
        String streamKey = props.getStreamKey();
        long resultTtlMs = props.getResultTtlMs();

        Map<String, String> body = new HashMap<>();
        body.put("auctionId", auctionId.toString());
        body.put("buyerId", buyerId.toString());
        body.put("amount", String.valueOf(amount));
        body.put("requestId", requestId);

        /*
        * MapRecord<K, HK, HV>
        * K: Stream Key 타입 - stream:bids
        * HK: Hash Key(필드 이름) 타입 - auctionId
        * HV: Hash Value(필드 값) 타입 - 1000
         * */
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .in(streamKey)
                .ofMap(body);

        RecordId id = redisTemplate.opsForStream().add(record);
        if (id == null) {
            String resultKey = BidStreamSupport.resultKey(buyerId, requestId);
            bidResultStore.put(
                    resultKey,
                    BidProcessingResult.error(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Failed to publish bid stream"),
                    Duration.ofMillis(resultTtlMs)
            );
        }
    }

    // 시스템 에러 처리
    private BidCreateResponse resolveOrThrow(BidProcessingResult result) {
        if (result.isSuccess()) {
            return result.response();
        }
        if (result.isError()) {
            if (ErrorCode.INTERNAL_SERVER_ERROR.name().equals(result.errorCode())) {
                throw new RuntimeException(result.errorMessage() != null ? result.errorMessage() : "Bid failed");
            }

            try {
                ErrorCode code = ErrorCode.valueOf(result.errorCode());
                throw new BusinessException(code);
            } catch (IllegalArgumentException ignored) {
                throw new RuntimeException(result.errorMessage() != null ? result.errorMessage() : "Bid failed");
            }
        }
        throw new BusinessException(ErrorCode.BID_QUEUE_TIMEOUT);
    }

    private BidCreateResponse buildBidCreateResponse(Bid bid, boolean extensionApplied) {
        var auction = bid.getAuction();

        int currentHighest = auction.getCurrentPrice();
        int minAcceptable = currentHighest + auction.getBidIncrement();

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

    private void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}