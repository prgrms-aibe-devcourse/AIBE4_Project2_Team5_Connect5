package kr.eolmago.service.auction.constants;

public final class AuctionConstants {
    public static final int EXTENSION_THRESHOLD_SECONDS = 300; // 연장 발동 임계값(5분)
    public static final int EXTENSION_DURATION_SECONDS = 300;  // 연장 시간(5분)
    public static final int MAX_REMAINING_SECONDS = 1800; // 남은시간 상한(30분)
    public static final int HARD_MAX_EXTENSION_HOURS = 12; // 최대 연장 시간(12시간)
    public static final int MAX_BID_AMOUNT = 10_000_000; // 입찰 금액 상한
    public static final int SWEEP_PAGE_SIZE = 500;
    public static final long RESULT_WAIT_POLL_MS = 50L;
}
