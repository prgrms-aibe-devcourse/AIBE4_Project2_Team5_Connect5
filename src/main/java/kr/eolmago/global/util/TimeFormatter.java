package kr.eolmago.global.util;

import java.time.Duration;
import java.time.OffsetDateTime;

public class TimeFormatter {

    // 남은 시간 포맷팅 메서드
    public static String formatRemainingTime(OffsetDateTime endAt) {
        if (endAt == null) {
            return "";
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (endAt.isBefore(now)) {
            return "종료";
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

    private TimeFormatter() {
        // 인스턴스화 방지
    }
}
