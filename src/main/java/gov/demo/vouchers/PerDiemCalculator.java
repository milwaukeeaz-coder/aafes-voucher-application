package gov.demo.vouchers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class PerDiemCalculator {
    private PerDiemCalculator() {}

    public static long days(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end date is before start date");
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    public static long perDiemCents(LocalDate start, LocalDate end, long dailyRateCents) {
        return days(start, end) * dailyRateCents;
    }
}
