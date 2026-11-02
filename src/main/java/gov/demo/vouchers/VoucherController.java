package gov.demo.vouchers;

import gov.demo.vouchers.Models.Traveler;
import gov.demo.vouchers.Models.Voucher;
import gov.demo.vouchers.Models.VoucherSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
}
