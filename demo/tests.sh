#!/usr/bin/env bash
cd "$(git rev-parse --show-toplevel)" || exit 1
source demo/env.sh
if [ -f mvnw ]; then MVN='sh ./mvnw'; else MVN=mvn; fi
$MVN test 2>&1 | grep -E 'Tests run:|FAIL|BUILD' | tail -12
