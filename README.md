# Travel Voucher Service

Spring Boot service for travel vouchers. It uses Java 21, an in-memory H2 database, and a small web UI on port 8080.

## Setup

1. Install Java 21 and Maven. Spring Boot 3.3 does not run on Java 25.
   ```bash
   brew install openjdk@21 maven
   ```
2. From this directory, start the app. The first run downloads dependencies.
   ```bash
   ./demo/start-app.sh
   ```
   To start it without the script: `mvn spring-boot:run`.
3. When the script prints `Ready at http://localhost:8080`, open that URL. Seeded accounts all use the password `Password1!`:
   - `alice` and `bob` — TRAVELER
   - `carol` — APPROVER
   - `admin` — ADMIN
4. Stop the app with `./demo/stop-app.sh`.

Startup creates the schema and seed data. Both disappear when the process stops.

Most `/api` routes require `Authorization: Bearer <token>` from `POST /api/login`. These do not: `POST /api/login`, `/api/metrics/**`, and `GET /api/admin/export-all` (that route checks the `X-Api-Key` header against the key in `AdminController`).

## Endpoints

- `POST /api/login` — Check a username and password and return a bearer token and role.
- `GET /api/vouchers` — List the signed-in traveler's vouchers with destination, dates, status, traveler name, and total cents.
- `GET /api/vouchers/search` — Search the signed-in traveler's vouchers by the `destination` query parameter.
- `GET /api/vouchers/{id}` — Return one voucher, or 404 when that id does not exist.
- `GET /api/vouchers/{id}/total` — Return per-diem cents, line-item cents, and the combined total for one voucher.
- `POST /api/vouchers/{id}/approve` — Set a voucher's status to APPROVED; the caller must have the APPROVER role.
- `GET /api/admin/users` — List every traveler account.
- `GET /api/admin/export-all` — Return the voucher count when the `X-Api-Key` header matches the admin key.
- `GET /api/reports/summary` — Return voucher counts and total cents grouped by status.
- `GET /api/metrics/queries` — Return how many SQL queries this process has counted.
- `POST /api/metrics/reset` — Reset the SQL query counter to zero.
- `POST /api/demo/tests` — Run `demo/tests.sh` and return its text output.
- `POST /api/demo/security-check` — Run `demo/security-check.sh` and return its text output.
- `POST /api/demo/bench` — Run `demo/bench.sh` and return its text output.
