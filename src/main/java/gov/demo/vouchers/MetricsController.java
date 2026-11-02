package gov.demo.vouchers;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Demo tooling used by demo/bench.sh. Not business logic. */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    @GetMapping("/queries")
    public Map<String, Object> queries() {
        return Map.of("queries", QueryCounter.count());
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        QueryCounter.reset();
        return Map.of("queries", 0);
    }
}
