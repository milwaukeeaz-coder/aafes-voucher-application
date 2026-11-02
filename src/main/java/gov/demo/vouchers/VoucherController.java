package gov.demo.vouchers;

import gov.demo.vouchers.Models.Traveler;
import gov.demo.vouchers.Models.Voucher;
import gov.demo.vouchers.Models.VoucherExportRow;
import gov.demo.vouchers.Models.VoucherSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    private static final Logger log = LoggerFactory.getLogger(VoucherController.class);
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_PAGE = 10_000;

    private final VoucherRepository repo;

    public VoucherController(VoucherRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<VoucherSummary> list(@RequestAttribute("user") Traveler user) {
        List<VoucherSummary> out = new ArrayList<>();
        for (Voucher v : repo.findAll()) {
            if (v.travelerId() != user.id()) {
                continue;
            }
            String name = repo.travelerName(v.travelerId());
            long items = repo.lineItemTotal(v.id());
            long perDiem = PerDiemCalculator.perDiemCents(v.startDate(), v.endDate(), v.dailyRateCents());
            out.add(new VoucherSummary(v.id(), v.destination(), v.startDate(), v.endDate(),
                v.status(), name, perDiem + items));
        }
        return out;
    }

    /**
     * CSV of the logged-in user's vouchers.
     * Expected impact: 1 SQL query per request (paged join), 0 queries per row.
     */
    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@RequestAttribute("user") Traveler user,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(required = false) Integer size) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (page < 0 || page > MAX_PAGE || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return ResponseEntity.badRequest().body("invalid request\n");
        }
        List<VoucherExportRow> rows = repo.findForExport(user.id(), pageSize, page * pageSize);
        log.info("AUDIT user={} action={} target={}", user.username(), "export.csv", "vouchers");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vouchers.csv\"")
            .body(toCsv(rows));
    }

    @GetMapping("/search")
    public List<Voucher> search(@RequestParam String destination, @RequestAttribute("user") Traveler user) {
        return repo.searchByDestination(user.id(), destination);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> get(@PathVariable long id) {
        Optional<Voucher> voucher = repo.findById(id);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(voucher.get());
    }

    @GetMapping("/{id}/total")
    public ResponseEntity<Object> total(@PathVariable long id) {
        Optional<Voucher> voucher = repo.findById(id);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Voucher v = voucher.get();
        long perDiem = PerDiemCalculator.perDiemCents(v.startDate(), v.endDate(), v.dailyRateCents());
        long items = repo.lineItemTotal(v.id());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("perDiemCents", perDiem);
        body.put("lineItemsCents", items);
        body.put("totalCents", perDiem + items);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Object> approve(@PathVariable long id, @RequestAttribute("user") Traveler user) {
        if (!"APPROVER".equals(user.role())) {
            return ResponseEntity.status(403).body(Map.of("error", "approver role required"));
        }
        if (repo.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        repo.updateStatus(id, "APPROVED");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("status", "APPROVED");
        return ResponseEntity.ok(body);
    }

    private static String toCsv(List<VoucherExportRow> rows) {
        StringBuilder out = new StringBuilder();
        out.append("id,destination,start,end,status,total\n");
        for (VoucherExportRow row : rows) {
            long totalCents = PerDiemCalculator.perDiemCents(row.startDate(), row.endDate(), row.dailyRateCents())
                + row.lineItemsCents();
            out.append(row.id()).append(',')
                .append(csv(row.destination())).append(',')
                .append(row.startDate()).append(',')
                .append(row.endDate()).append(',')
                .append(csv(row.status())).append(',')
                .append(dollars(totalCents)).append('\n');
        }
        return out.toString();
    }

    private static String dollars(long cents) {
        long abs = Math.abs(cents);
        return (cents < 0 ? "-" : "") + (abs / 100) + "." + String.format("%02d", abs % 100);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
            || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }
}
