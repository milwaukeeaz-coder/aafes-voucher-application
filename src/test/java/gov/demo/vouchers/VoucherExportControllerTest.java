package gov.demo.vouchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class VoucherExportControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void exportCsvReturnsOnlyTheLoggedInUsersVouchers() throws Exception {
        String body = export("bob", "");

        assertTrue(body.startsWith("id,destination,start,end,status,total\n"));
        assertTrue(body.contains("1501,Tokyo,2026-01-06,2026-01-07,DRAFT,611.00\n"));
        assertFalse(body.contains("\n1,Tokyo,"), "alice's voucher must not appear in bob's export");
        assertEquals(51, body.lines().count(), "default page size is 50 data rows");
    }

    @Test
    void exportCsvHonorsPageAndSize() throws Exception {
        String body = export("bob", "?page=1&size=1");

        assertEquals("""
            id,destination,start,end,status,total
            1502,Norfolk,2026-01-07,2026-01-09,APPROVED,782.00
            """, body);
    }

    @Test
    void exportCsvRequiresLoginAndBoundsPageSize() throws Exception {
        mvc.perform(get("/api/vouchers/export.csv"))
            .andExpect(status().isUnauthorized());

        String token = login("alice");
        mvc.perform(get("/api/vouchers/export.csv")
                .param("size", "201")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    private String export(String username, String query) throws Exception {
        String token = login(username);
        return mvc.perform(get("/api/vouchers/export.csv" + query)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private String login(String username) throws Exception {
        String response = mvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Password1!\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode body = json.readTree(response);
        return body.get("token").asText();
    }
}
