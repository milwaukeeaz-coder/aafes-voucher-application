# Cursor 3 demo: Travel Voucher Service

A small Spring Boot 3 app (Java 21, in-memory H2, no Docker) built for a three-act Cursor demo. Every act has a one-command scoreboard that changes visibly after the agent's fix.

## Setup (once)
1. Install Java 21 (`brew install openjdk@21`) and Maven (`brew install maven`), or copy `mvnw`, `mvnw.cmd` and `.mvn/` from any Maven project into this folder. Spring Boot 3.3 does not run on Java 25.
2. In this folder:
   ```bash
   git init && git add -A && git commit -m 'Demo baseline'
   git branch -M demo-base && git tag demo-start && git checkout -b demo-run
   chmod +x demo/*.sh
   ```
3. Start the app: `./demo/start-app.sh` (first run downloads dependencies). Open http://localhost:8080, log in as alice (password `Password1!`).
4. Record the "before" state for each act (below). Practice once before the real run.

Users (all with password `Password1!`): alice and bob (TRAVELER), carol (APPROVER), admin (ADMIN). Voucher 1 is alice's. Voucher 1501 is bob's, and its purpose contains the marker CONFIDENTIAL-BOB.

## Scoreboards
| Act | Command | Before | After |
|---|---|---|---|
| 1 | `./demo/tests.sh` | 3 of 4 tests fail | 4 of 4 pass |
| 1 | UI: Download CSV button | "not available yet" | file downloads |
| 2 | `./demo/security-check.sh` | 0/7 | 7/7 |
| 3 | `./demo/bench.sh` | about 3,000 and 5,000 queries, seconds of latency | a handful of queries |

Restart the app (`./demo/stop-app.sh`, `./demo/start-app.sh`) after code changes, and re-run the scoreboard.

## Act 1: Developer productivity
Say: this app has four tests, and three of them fail. Show `./demo/tests.sh`.

Plan (read-only):
```
Map this Spring Boot project: controllers, repository, per diem calculation, and tests. Propose a plan to fix the failing tests and add a CSV export of the caller's vouchers. Do not edit files.
```
Agent A:
```
PerDiemCalculatorTest has failing tests. Find the root cause in production code, fix it, and run the tests.
```
Agent B:
```
Add GET /api/vouchers/export.csv that returns the logged-in user's vouchers as CSV (id, destination, start, end, status, total in dollars). Add a controller test or unit test. Follow the rules in .cursor/rules.
```
Agent C:
```
Write a README.md with setup steps and a one-line description of every endpoint under /api.
```
Proof: `./demo/tests.sh` shows 4 of 4 passing. Reload the page: totals now include every trip day. Click Download CSV.

## Act 2: STIG compliance at the development stage
Say: it maps to Application Security and Development STIG themes. Cursor does not certify compliance.

Review (read-only):
```
Review this codebase against .cursor/rules/stig-java.mdc. List findings by severity with file and line. Do not change anything yet.
```
Fixes, one at a time:
```
Fix SQL injection in VoucherRepository.searchByDestination with a parameterized query. Make GET /api/vouchers/{id} and /{id}/total return 404 unless the caller owns the voucher or has the APPROVER or ADMIN role.
```
```
Remove the hardcoded API key from AdminController and read it from the ADMIN_API_KEY environment variable. Replace MD5 in PasswordHasher with BCrypt from spring-security-crypto. Keep DataSeeder working.
```
```
Restrict /api/admin/users to the ADMIN role and stop returning password hashes. Make GlobalErrorHandler return a generic message with no stack trace and log the details instead. Add AUDIT log lines for voucher approval and admin access.
```
Proof: restart, then `./demo/security-check.sh` goes from 0/7 to 7/7. Log in again in the UI to confirm the app still works (BCrypt makes startup slightly slower).

## Act 3: FinOps
Say: the cost numbers use assumptions printed at the bottom of `bench.sh`. State them out loud and override with `REQS_PER_DAY` and `USD_PER_MILLION_QUERIES`.

Audit (read-only):
```
Audit this codebase for runtime cost against .cursor/rules/finops-java.mdc. Rank findings by estimated impact. Do not change anything yet.
```
Fixes:
```
Rewrite the voucher list endpoint to use a single JOIN query scoped to the caller, with page and size parameters (default size 50). Keep the response a JSON array of the same fields.
```
```
Rewrite /api/reports/summary to use one GROUP BY query, and cache the result for 60 seconds. Add indexes to schema.sql for vouchers.traveler_id and line_items.voucher_id. Remove per-row INFO logging from VoucherRepository.
```
Proof: restart, run `./demo/bench.sh` again. Note that the first summary call after a restart is uncached; run it twice to show the cache.

## Reset after every practice run
```bash
./demo/reset.sh
```
This stops the app, removes agent worktrees and stray branches, and resets demo-run to the `demo-start` tag. The database is in memory, so it resets on every start. Then close editor tabs and delete old agent threads in Cursor.

If you change the baseline, commit it on demo-base, then `git tag -f demo-start`.

## Merging worktree agents
See the run book. In short: commit in each worktree, then `git merge --no-ff <branch>` into demo-run.
