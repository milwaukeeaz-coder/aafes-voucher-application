package gov.demo.vouchers;

import gov.demo.vouchers.Models.Voucher;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {
    private final VoucherRepository repo;

    public ReportController(VoucherRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/reports/summary")
    public Map<String, Object> summary() {
        Map<String, long[]> byStatus = new TreeMap<>();
        long count = 0;
        for (Voucher v : repo.findAll()) {
            long total = PerDiemCalculator.perDiemCents(v.startDate(), v.endDate(), v.dailyRateCents())
                + repo.lineItemTotal(v.id());
            long[] agg = byStatus.computeIfAbsent(v.status(), k -> new long[2]);
            agg[0]++;
            agg[1] += total;
            count++;
        }
        Map<String, Object> statuses = new LinkedHashMap<>();
        byStatus.forEach((status, agg) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("count", agg[0]);
            row.put("totalCents", agg[1]);
            statuses.put(status, row);
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("voucherCount", count);
        out.put("byStatus", statuses);
        return out;
    }
}
