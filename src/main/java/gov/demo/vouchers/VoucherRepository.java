package gov.demo.vouchers;

import gov.demo.vouchers.Models.Traveler;
import gov.demo.vouchers.Models.Voucher;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class VoucherRepository {
    private static final Logger log = LoggerFactory.getLogger(VoucherRepository.class);

    private static final RowMapper<Voucher> VOUCHER = (rs, i) -> new Voucher(
        rs.getLong("id"), rs.getLong("traveler_id"), rs.getString("destination"), rs.getString("purpose"),
        rs.getString("status"), rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(),
        rs.getLong("daily_rate_cents"));

    private static final RowMapper<Traveler> TRAVELER = (rs, i) -> new Traveler(
        rs.getLong("id"), rs.getString("username"), rs.getString("display_name"),
        rs.getString("password_hash"), rs.getString("role"));

    private final JdbcTemplate jdbc;

    public VoucherRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Voucher> findAll() {
        return jdbc.query("select * from vouchers", VOUCHER);
    }

    public Optional<Voucher> findById(long id) {
        return jdbc.query("select * from vouchers where id = ?", VOUCHER, id).stream().findFirst();
    }

    public List<Voucher> searchByDestination(long travelerId, String text) {
        String sql = "select * from vouchers where traveler_id = " + travelerId
            + " and destination like '%" + text + "%'";
        return jdbc.query(sql, VOUCHER);
    }

    public String travelerName(long travelerId) {
        log.info("Loading traveler {}", travelerId);
        return jdbc.queryForObject("select display_name from travelers where id = ?", String.class, travelerId);
    }

    public long lineItemTotal(long voucherId) {
        log.info("Loading line items for voucher {}", voucherId);
        Long total = jdbc.queryForObject(
            "select coalesce(sum(amount_cents), 0) from line_items where voucher_id = ?", Long.class, voucherId);
        return total == null ? 0 : total;
    }

    public void updateStatus(long id, String status) {
        jdbc.update("update vouchers set status = ? where id = ?", status, id);
    }

    public Optional<Traveler> findTravelerByUsername(String username) {
        return jdbc.query("select * from travelers where username = ?", TRAVELER, username).stream().findFirst();
    }

    public List<Traveler> findAllTravelers() {
        return jdbc.query("select * from travelers", TRAVELER);
    }
}
