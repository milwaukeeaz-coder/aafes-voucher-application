package gov.demo.vouchers;

import java.time.LocalDate;

public final class Models {
    private Models() {}

    public record Traveler(long id, String username, String displayName, String passwordHash, String role) {}

    public record Voucher(long id, long travelerId, String destination, String purpose, String status,
                          LocalDate startDate, LocalDate endDate, long dailyRateCents) {}

    public record VoucherSummary(long id, String destination, LocalDate startDate, LocalDate endDate,
                                 String status, String travelerName, long totalCents) {}

    /** One voucher row for CSV export. Totals are computed from these fields, not extra queries. */
    public record VoucherExportRow(long id, String destination, LocalDate startDate, LocalDate endDate,
                                   String status, long dailyRateCents, long lineItemsCents) {}
}
