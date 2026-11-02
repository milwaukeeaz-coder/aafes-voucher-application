package gov.demo.vouchers;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Seeds demo data: 12 travelers, 5,000 vouchers, 10,000 line items. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String[] CITIES = {"Ramstein", "Tokyo", "Norfolk", "San Diego", "Honolulu", "Stuttgart"};

    private final JdbcTemplate jdbc;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        String hash = PasswordHasher.hash("Password1!");
        List<Object[]> travelers = new ArrayList<>();
        travelers.add(new Object[] {1L, "alice", "Alice Anderson", hash, "TRAVELER"});
        travelers.add(new Object[] {2L, "bob", "Bob Baker", hash, "TRAVELER"});
        travelers.add(new Object[] {3L, "carol", "Carol Chen", hash, "APPROVER"});
        travelers.add(new Object[] {4L, "admin", "Site Admin", hash, "ADMIN"});
        for (int i = 1; i <= 8; i++) {
            travelers.add(new Object[] {4L + i, "t0" + i, "Traveler " + i, hash, "TRAVELER"});
        }
        jdbc.batchUpdate("insert into travelers (id, username, display_name, password_hash, role) values (?,?,?,?,?)",
            travelers);

        List<Object[]> vouchers = new ArrayList<>();
        List<Object[]> items = new ArrayList<>();
        for (int i = 1; i <= 5000; i++) {
            long owner = i <= 1500 ? 1L : i <= 2500 ? 2L : 5L + (i % 8);
            String purpose = owner == 2L ? "CONFIDENTIAL-BOB mission brief" : "Routine travel";
            String status = i % 3 == 0 ? "SUBMITTED" : i % 3 == 1 ? "DRAFT" : "APPROVED";
            LocalDate start = LocalDate.of(2026, 1, 5).plusDays(i % 300);
            LocalDate end = start.plusDays(i % 5);
            vouchers.add(new Object[] {(long) i, owner, CITIES[i % CITIES.length], purpose, status,
                Date.valueOf(start), Date.valueOf(end), 16100L});
            items.add(new Object[] {2L * i, (long) i, "AIRFARE", 30000L + (i % 7) * 1000L});
            items.add(new Object[] {2L * i + 1, (long) i, "LODGING", 12000L});
        }
        jdbc.batchUpdate("insert into vouchers (id, traveler_id, destination, purpose, status, start_date, "
            + "end_date, daily_rate_cents) values (?,?,?,?,?,?,?,?)", vouchers);
        jdbc.batchUpdate("insert into line_items (id, voucher_id, category, amount_cents) values (?,?,?,?)", items);
        log.info("Seeded {} travelers, {} vouchers, {} line items", travelers.size(), vouchers.size(), items.size());
    }
}
