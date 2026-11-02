#!/usr/bin/env bash
# STIG-themed scoreboard. Needs the app running. Prints PASS or FAIL for seven probes.
cd "$(git rev-parse --show-toplevel)" || exit 1
source demo/env.sh
login() {
  curl -s -X POST "$BASE/api/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"Password1!\"}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}
ALICE=$(login alice); CAROL=$(login carol)
if [ -z "$ALICE" ] || [ -z "$CAROL" ]; then echo 'Login failed. Is the app running?'; exit 1; fi
SCORE=0; TOTAL=7
result() { if [ "$1" = pass ]; then SCORE=$((SCORE+1)); printf '  \033[32mPASS\033[0m  %s\n' "$2"; else printf '  \033[31mFAIL\033[0m  %s\n' "$2"; fi; }
echo 'STIG-themed scoreboard'

B=$(curl -s -G "$BASE/api/vouchers/search" --data-urlencode "destination=%' OR '1'='1' --" -H "Authorization: Bearer $ALICE")
echo "$B" | grep -q 'CONFIDENTIAL-BOB' && result fail '1. SQL injection in voucher search' || result pass '1. SQL injection in voucher search'

B=$(curl -s "$BASE/api/vouchers/1501" -H "Authorization: Bearer $ALICE")
echo "$B" | grep -q 'CONFIDENTIAL-BOB' && result fail "2. Traveler can read another traveler's voucher" || result pass "2. Traveler can read another traveler's voucher"

grep -rq 'demo-admin-key' src/main && result fail '3. Hardcoded API key in source' || result pass '3. Hardcoded API key in source'

grep -rqiE 'MessageDigest|MD5' src/main/java && result fail '4. Weak password hashing (MD5)' || result pass '4. Weak password hashing (MD5)'

C=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/admin/users" -H "Authorization: Bearer $ALICE")
{ [ "$C" = 401 ] || [ "$C" = 403 ]; } && result pass '5. Admin endpoint open to travelers' || result fail '5. Admin endpoint open to travelers'

B=$(curl -s "$BASE/api/vouchers/abc" -H "Authorization: Bearer $ALICE")
echo "$B" | grep -qE 'gov\.demo|Exception' && result fail '6. Stack traces returned to clients' || result pass '6. Stack traces returned to clients'

curl -s -X POST "$BASE/api/vouchers/1/approve" -H "Authorization: Bearer $CAROL" >/dev/null
grep -q 'AUDIT' demo/run.log && result pass '7. Approval is written to an audit log' || result fail '7. Approval is written to an audit log'

echo "Score: $SCORE/$TOTAL"
