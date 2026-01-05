package kr.eolmago.global.util;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 시간 포맷팅 유틸리티 클래스
 */
public class TimeFormatter {

    private TimeFormatter() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * 경매 남은 시간 포맷팅
     * - 1시간 이상: "n일 n시간" (예: "2일 5시간")
     * - 1시간 미만: "n분" (예: "45분")
     * @param endAt 경매 종료 시간
     * @return 포맷팅된 남은 시간 문자열
     */
    public static String formatRemainingTime(OffsetDateTime endAt) {
        if (endAt == null) {
            return "";
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (endAt.isBefore(now)) {
            return "종료됨";
        }

        Duration duration = Duration.between(now, endAt);
        long totalMinutes = duration.toMinutes();

        // 1시간 미만 (60분 미만)
        if (totalMinutes < 60) {
            return totalMinutes + "분";
        }

        // 1시간 이상
        long days = duration.toDays();
        long hours = duration.toHoursPart();

        if (days > 0) {
            return String.format("%d일 %d시간", days, hours);
        } else {
            return hours + "시간";
        }
    }

    /**
     * 총 남은 시간(분 단위)을 반환
     * @param endAt 경매 종료 시간
     * @return 남은 시간(분)
     */
    public static long getRemainingMinutes(OffsetDateTime endAt) {
        if (endAt == null) {
            return 0;
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (endAt.isBefore(now)) {
            return 0;
        }

        Duration duration = Duration.between(now, endAt);
        return duration.toMinutes();
    }
}
