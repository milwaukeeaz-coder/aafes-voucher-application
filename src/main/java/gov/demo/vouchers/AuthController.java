package gov.demo.vouchers;

import gov.demo.vouchers.Models.Traveler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    public record LoginRequest(String username, String password) {}

    private final VoucherRepository repo;
    private final TokenStore tokens;

    public AuthController(VoucherRepository repo, TokenStore tokens) {
        this.repo = repo;
        this.tokens = tokens;
    }

    @PostMapping("/api/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest req) {
        Optional<Traveler> user = repo.findTravelerByUsername(req.username());
        if (user.isEmpty() || !PasswordHasher.matches(req.password(), user.get().passwordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", tokens.issue(user.get()));
        body.put("role", user.get().role());
        return ResponseEntity.ok(body);
    }
}
