package kr.eolmago.global.util;

// 입찰 단위 계산 유틸리티
public class BidIncrementCalculator {

    // 현재가 기준으로 입찰 단위를 자동 계산
    public static int calculate(int currentPrice) {
        if (currentPrice < 200_000) {
            return 1_000;
        } else if (currentPrice < 1_000_000) {
            return 5_000;
        } else if (currentPrice < 3_000_000) {
            return 10_000;
        } else if (currentPrice < 10_000_000) {
            return 50_000;
        } else if (currentPrice <= 30_000_000) {
            return 100_000;
        } else {
            // 30,000,000원 초과 시에도 100,000원 단위 유지
            return 100_000;
        }
    }

    private BidIncrementCalculator() {
        // 인스턴스화 방지
    }
}
