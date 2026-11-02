package gov.demo.vouchers;

import gov.demo.vouchers.Models.Traveler;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final String ADMIN_API_KEY = "demo-admin-key-123";

    private final VoucherRepository repo;

    public AdminController(VoucherRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/users")
    public List<Traveler> users() {
        return repo.findAllTravelers();
    }

    @GetMapping("/export-all")
    public ResponseEntity<Object> exportAll(@RequestHeader(value = "X-Api-Key", required = false) String key) {
        if (!ADMIN_API_KEY.equals(key)) {
            return ResponseEntity.status(401).body(Map.of("error", "bad api key"));
        }
        return ResponseEntity.ok(Map.of("voucherCount", repo.findAll().size()));
    }
}
