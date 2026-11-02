#!/usr/bin/env bash
# FinOps scoreboard. Needs the app running.
# The cost figures use ASSUMPTIONS, edit them or override with environment variables.
cd "$(git rev-parse --show-toplevel)" || exit 1
source demo/env.sh
REQS_PER_DAY="${REQS_PER_DAY:-20000}"                 # assumption: calls per day to each endpoint
USD_PER_MILLION_QUERIES="${USD_PER_MILLION_QUERIES:-0.20}"   # assumption: illustrative database cost
TOKEN=$(curl -s -X POST "$BASE/api/login" -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Password1!"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then echo 'Login failed. Is the app running?'; exit 1; fi
printf '%-26s %10s %10s %10s %14s\n' endpoint queries seconds log_lines est_usd_month
measure() {
  curl -s -X POST "$BASE/api/metrics/reset" >/dev/null
  L0=$(wc -l < demo/run.log)
  T=$(curl -s -o /dev/null -w '%{time_total}' "$BASE$1" -H "Authorization: Bearer $TOKEN")
  Q=$(curl -s "$BASE/api/metrics/queries" | sed -n 's/.*"queries":\([0-9]*\).*/\1/p')
  L1=$(wc -l < demo/run.log)
  COST=$(awk -v q="$Q" -v r="$REQS_PER_DAY" -v p="$USD_PER_MILLION_QUERIES" 'BEGIN{printf "%.2f", q*r*30/1000000*p}')
  printf '%-26s %10s %10s %10s %14s\n' "$1" "$Q" "$T" "$((L1-L0))" "$COST"
}
measure /api/vouchers
measure /api/reports/summary
echo "Assumptions: $REQS_PER_DAY requests/day per endpoint, \$$USD_PER_MILLION_QUERIES per million queries, 30 days. Illustrative only."
