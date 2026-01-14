package kr.eolmago.service.auction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import kr.eolmago.dto.api.auction.response.BidCreateResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.service.auction.stream.BidProcessingResult;
import kr.eolmago.service.auction.stream.BidResultStore;
import kr.eolmago.service.auction.stream.BidStreamProperties;
import kr.eolmago.service.auction.stream.BidStreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidStreamProcessor implements DisposableBean {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final BidCommandService bidCommandService;
    private final BidResultStore bidResultStore;

    private final BidStreamProperties props;

    private ExecutorService executor;
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    // BidStreamProperties
    private String streamKey;
    private String group;
    private long resultTtlMs;

    @PostConstruct
    public void start() {

        if (!props.isConsumerEnabled()) return;

        this.streamKey = props.getStreamKey();
        this.group = props.getGroup();
        this.resultTtlMs = props.getResultTtlMs();
        String consumerName = props.getConsumerName();
        int batchSize = props.getBatchSize();
        long pollTimeoutMs = props.getPollTimeoutMs();

        if (consumerName == null || consumerName.isBlank()) {
            consumerName = "c-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // consumer 1개 구성
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "bid-stream-consumer");
            t.setDaemon(true);
            return t;
        });

        // ConsumerGroup 생성
        ensureConsumerGroup(streamKey, group);

        RedisConnectionFactory cf = redisTemplate.getConnectionFactory();
        if (cf == null) {
            throw new BusinessException(ErrorCode.INFRA_REDIS_CONNECTION_FACTORY_MISSING);
        }

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(batchSize)
                        .pollTimeout(Duration.ofMillis(pollTimeoutMs))
                        .executor(executor)
                        .build();

        container = StreamMessageListenerContainer.create(cf, options);

        container.receive(
                Consumer.from(group, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                this::onMessage
        );

        container.start();
    }

    // 결과키 저장 후 ACK
    private void onMessage(MapRecord<String, String, String> message) {
        String recordId = message.getId().getValue();
        Map<String, String> v = message.getValue();

        String auctionIdStr = v.get("auctionId");
        String buyerIdStr = v.get("buyerId");
        String amountStr = v.get("amount");
        String requestId = v.get("requestId");

        UUID auctionId = null;
        UUID buyerId = null;
        int amount = 0;

        try {
            if (auctionIdStr == null || buyerIdStr == null || amountStr == null || requestId == null) { // 필수 값 하나라도 누락 시
                // 시스템 에러 결과 남기고 ACK
                writeSystemErrorAndAck(recordId, buyerIdStr, requestId, "Invalid stream payload", message);
                return;
            }

            auctionId = UUID.fromString(auctionIdStr);
            buyerId = UUID.fromString(buyerIdStr);
            amount = Integer.parseInt(amountStr);

            String resultKey = BidStreamSupport.resultKey(buyerId, requestId);

            try {
                BidCreateResponse response = bidCommandService.createBid(auctionId, buyerId, amount, requestId);
                bidResultStore.put( // 결과키 저장
                        resultKey,
                        BidProcessingResult.success(response),
                        Duration.ofMillis(resultTtlMs)
                );

            } catch (BusinessException be) {
                ErrorCode code = be.getErrorCode();
                String errorCode = (code != null ? code.name() : ErrorCode.INTERNAL_SERVER_ERROR.name());

                bidResultStore.put(
                        resultKey,
                        BidProcessingResult.error(errorCode, null),
                        Duration.ofMillis(resultTtlMs)
                );

            } catch (Exception e) {
                // 시스템 예외 시 결과 저장 후 ACK
                if (buyerId != null) {
                    bidResultStore.put(
                            resultKey,
                            BidProcessingResult.error(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()),
                            Duration.ofMillis(resultTtlMs)
                    );
                }
            }
        } catch (Exception parseOrOther) {
            writeSystemErrorAndAck(recordId, buyerIdStr, requestId, parseOrOther.getMessage(), message);
            return;
        }

        ack(message);
    }

    // 시스템 에러 후 ACK
    private void writeSystemErrorAndAck(String recordId, String buyerIdStr, String requestId, String msg, MapRecord<String, String, String> message) {
        try {
            if (buyerIdStr != null && requestId != null) {
                UUID buyerId = UUID.fromString(buyerIdStr);
                String resultKey = BidStreamSupport.resultKey(buyerId, requestId);

                bidResultStore.put(
                        resultKey,
                        BidProcessingResult.error(ErrorCode.INTERNAL_SERVER_ERROR.name(), msg),
                        Duration.ofMillis(resultTtlMs)
                );
            }
        } catch (Exception ignored) {
        } finally {
            ack(message);
        }
    }

    private void ack(MapRecord<String, String, String> message) {
        try {
            Long acked = redisTemplate.opsForStream().acknowledge(streamKey, group, message.getId());
            if (acked == null || acked == 0) {
                log.debug("[BID_STREAM] ack returned 0. id={}", message.getId().getValue());
            }
        } catch (Exception e) {
            log.warn("[BID_STREAM] ack failed. id={}", message.getId().getValue(), e);
        }
    }

    // XGROUP CREATE <stream> <group> $ MKSTREAM
    // Consumer Group 생성
    private void ensureConsumerGroup(String stream, String group) {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                byte[] create = "CREATE".getBytes(StandardCharsets.UTF_8);
                byte[] mkstream = "MKSTREAM".getBytes(StandardCharsets.UTF_8);
                byte[] dollar = "$".getBytes(StandardCharsets.UTF_8);
                byte[] strm = stream.getBytes(StandardCharsets.UTF_8);
                byte[] grp = group.getBytes(StandardCharsets.UTF_8);

                try {
                    // Lettuce/Spring이 예외를 래핑할 수 있으므로, BUSYGROUP는 cause 체인까지 확인
                    connection.execute("XGROUP", create, strm, grp, dollar, mkstream);
                } catch (Exception e) {
                    // BUSYGROUP Consumer Group name already exists
                    if (isBusyGroup(e)) {
                        return null; // 이미 존재하면 무시
                    }
                    throw e;
                }
                return null;
            });
        } catch (Exception e) {
            log.error("[BID_STREAM] consumer group 생성 실패. streamKey={}, group={}", stream, group, e);
            throw e;
        }
    }

    private boolean isBusyGroup(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("BUSYGROUP")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    @PreDestroy
    @Override
    public void destroy() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception ignored) {}

        try {
            if (executor != null) {
                executor.shutdownNow();
            }
        } catch (Exception ignored) {}
    }
}