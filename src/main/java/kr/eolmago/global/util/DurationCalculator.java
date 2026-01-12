package kr.eolmago.global.util;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class DurationCalculator {
    public static int calculateHoursBetween(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            return 0;
        }

        long totalSeconds = ChronoUnit.SECONDS.between(start, end);
        if (totalSeconds <= 0) {
            return 0;
        }

        // 올림
        long hours = (totalSeconds + 3600 - 1) / 3600;

        return (int) Math.max(1, hours);
    }
}
