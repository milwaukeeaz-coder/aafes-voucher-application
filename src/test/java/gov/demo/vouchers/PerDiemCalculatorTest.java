package gov.demo.vouchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PerDiemCalculatorTest {
    private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);
    private static final LocalDate JUNE_3 = LocalDate.of(2026, 6, 3);

    @Test
    void threeDayTripCountsThreeDays() {
        assertEquals(3, PerDiemCalculator.days(JUNE_1, JUNE_3));
    }

    @Test
    void singleDayTripCountsOneDay() {
        assertEquals(1, PerDiemCalculator.days(JUNE_1, JUNE_1));
    }

    @Test
    void perDiemForThreeDaysAtDailyRate() {
        assertEquals(48300, PerDiemCalculator.perDiemCents(JUNE_1, JUNE_3, 16100));
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PerDiemCalculator.days(JUNE_3, JUNE_1));
    }
}
