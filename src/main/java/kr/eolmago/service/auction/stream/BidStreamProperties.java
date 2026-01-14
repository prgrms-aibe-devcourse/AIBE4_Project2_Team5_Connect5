package kr.eolmago.service.auction.stream;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auction.bid.stream")
public class BidStreamProperties {

    private boolean consumerEnabled = true;

    private String streamKey = "stream:bids";
    private String group = "cg:bids";
    private String consumerName = "";

    private int batchSize = 50;
    private long pollTimeoutMs = 200;

    // TTL
    private long resultTtlMs = 600_000L;
    private long idempotencyTtlMs = 600_000L;
    // API 결과 대기 타임아웃
    private long apiWaitTimeoutMs = 1_500;

}
