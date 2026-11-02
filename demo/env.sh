# Source this file (the scripts do it for you).
# The app needs Java 17+; Java 21 is recommended. Spring Boot 3.3 does not support Java 25.
if [ -x /usr/libexec/java_home ]; then
  JH="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  if [ -n "$JH" ]; then export JAVA_HOME="$JH"; fi
fi
export BASE="${BASE:-http://localhost:8080}"
# Demo-only value. The STIG fix reads the admin key from this variable.
export ADMIN_API_KEY="local-demo-key-not-a-secret"
