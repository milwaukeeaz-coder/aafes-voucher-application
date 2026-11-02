#!/usr/bin/env bash
cd "$(git rev-parse --show-toplevel)" || exit 1
source demo/env.sh
if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
  echo 'Port 8080 is busy. Run demo/stop-app.sh or demo/reset.sh first.'; exit 1
fi
if [ -f mvnw ]; then MVN='sh ./mvnw'; else MVN=mvn; fi
$MVN -q spring-boot:run > demo/run.log 2>&1 &
echo 'Starting (first run downloads dependencies)...'
for i in $(seq 1 180); do
  if curl -sf "$BASE/api/metrics/queries" >/dev/null 2>&1; then echo "Ready at $BASE"; exit 0; fi
  sleep 1
done
echo 'App did not start in 3 minutes. See: tail -50 demo/run.log'; exit 1
