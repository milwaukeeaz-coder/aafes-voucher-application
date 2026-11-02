package gov.demo.vouchers;

import gov.demo.vouchers.Models.Traveler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TokenStore {
    private final Map<String, Traveler> tokens = new ConcurrentHashMap<>();

    public String issue(Traveler traveler) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, traveler);
        return token;
    }

    public Traveler lookup(String token) {
        return tokens.get(token);
    }
}
