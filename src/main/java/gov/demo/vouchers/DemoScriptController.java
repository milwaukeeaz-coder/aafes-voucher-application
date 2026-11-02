package gov.demo.vouchers;

import gov.demo.vouchers.Models.Traveler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runs the three fixed demo scoreboard scripts and returns their text.
 * Each of these endpoints runs 0 SQL queries. tests.sh shells out to Maven.
 * security-check.sh and bench.sh call other HTTP endpoints, and those endpoints run their own queries.
 * Only one script runs at a time so the scoreboards do not reset each other's metrics.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoScriptController {
    private static final Logger log = LoggerFactory.getLogger(DemoScriptController.class);
    /** Kept off the console so security-check.sh does not treat a script run as an approval audit. */
    private static final Logger audit = LoggerFactory.getLogger("gov.demo.vouchers.ScriptAudit");
    private static final Set<String> ALLOWED = Set.of("tests.sh", "security-check.sh", "bench.sh");
    private static final int MAX_OUTPUT_CHARS = 32_000;
    private static final long TEST_TIMEOUT_SECONDS = 180;
    private static final long SCOREBOARD_TIMEOUT_SECONDS = 90;

    private final ReentrantLock runLock = new ReentrantLock();

    @PostMapping("/tests")
    public ResponseEntity<Map<String, Object>> tests(@RequestAttribute("user") Traveler user) {
        return run(user, "tests.sh", TEST_TIMEOUT_SECONDS);
    }

    @PostMapping("/security-check")
    public ResponseEntity<Map<String, Object>> securityCheck(@RequestAttribute("user") Traveler user) {
        return run(user, "security-check.sh", SCOREBOARD_TIMEOUT_SECONDS);
    }

    @PostMapping("/bench")
    public ResponseEntity<Map<String, Object>> bench(@RequestAttribute("user") Traveler user) {
        return run(user, "bench.sh", SCOREBOARD_TIMEOUT_SECONDS);
    }

    private ResponseEntity<Map<String, Object>> run(Traveler user, String scriptName, long timeoutSeconds) {
        if (!runLock.tryLock()) {
            return ResponseEntity.status(409).body(Map.of("error", "a scoreboard is already running"));
        }
        Process process = null;
        try {
            audit.info("AUDIT user={} action=run-demo-script target={}", user.username(), scriptName);
            Path script = resolve(scriptName);
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", script.toString());
            builder.redirectErrorStream(true);
            builder.directory(script.getParent().getParent().toFile());
            process = builder.start();
            StringBuffer output = new StringBuffer();
            Process running = process;
            Thread reader = Thread.ofVirtual().start(() -> readBounded(running.getInputStream(), output));
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                killTree(process.toHandle());
                process.waitFor(5, TimeUnit.SECONDS);
            }
            reader.join(2_000);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("script", scriptName);
            body.put("exitCode", finished ? process.exitValue() : -1);
            body.put("timedOut", !finished);
            body.put("output", stripAnsi(output.toString()));
            return ResponseEntity.ok(body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                killTree(process.toHandle());
            }
            log.warn("demo script interrupted script={}", scriptName);
            return ResponseEntity.status(500).body(Map.of("error", "could not run script"));
        } catch (Exception e) {
            if (process != null) {
                killTree(process.toHandle());
            }
            log.warn("demo script failed script={}", scriptName, e);
            return ResponseEntity.status(500).body(Map.of("error", "could not run script"));
        } finally {
            runLock.unlock();
        }
    }

    private static Path resolve(String scriptName) {
        if (!ALLOWED.contains(scriptName)) {
            throw new IllegalArgumentException("unknown script");
        }
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("demo").resolve(scriptName).normalize();
            if (candidate.getParent().endsWith("demo") && Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("demo script not found");
    }

    private static void readBounded(InputStream in, StringBuffer output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean truncated = false;
            while ((line = reader.readLine()) != null) {
                if (truncated || output.length() + line.length() + 1 > MAX_OUTPUT_CHARS) {
                    truncated = true;
                    continue;
                }
                output.append(line).append('\n');
            }
            if (truncated) {
                output.append("[output truncated]\n");
            }
        } catch (IOException e) {
            output.append("[output incomplete]\n");
        }
    }

    private static void killTree(ProcessHandle handle) {
        handle.descendants().forEach(ProcessHandle::destroyForcibly);
        handle.destroyForcibly();
    }

    static String stripAnsi(String text) {
        return text.replaceAll("\u001B\\[[0-?]*[ -/]*[@-~]", "");
    }
}
