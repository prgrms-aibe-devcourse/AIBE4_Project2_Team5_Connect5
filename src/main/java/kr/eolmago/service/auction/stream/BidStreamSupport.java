package kr.eolmago.service.auction.stream;

import java.util.UUID;

public final class BidStreamSupport {

    private BidStreamSupport() {}

    // bid:result:{buyerId}:{requestId}
    public static String resultKey(UUID buyerId, String requestId) {
        return "bid:result:" + buyerId + ":" + requestId;
    }

    // bid:publish:{buyerId}:{requestId}
    public static String publishKey(UUID buyerId, String requestId) {
        return "bid:publish:" + buyerId + ":" + requestId;
    }
}