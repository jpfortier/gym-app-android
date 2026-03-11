#!/usr/bin/env bash
# Test POST /chat with sample audio against the gym backend.
# Run from project root. Backend must be running (make run in ../gym).
#
# Usage:
#   ./scripts/test-backend-chat.sh                    # uses base.url from local.properties
#   ./scripts/test-backend-chat.sh https://127.0.0.1:8081
#   ./scripts/test-backend-chat.sh http://127.0.0.1:8081  # if backend runs without TLS

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SAMPLES_DIR="$PROJECT_ROOT/app/src/main/assets/samples"

# Sample: "Close grip bench 130" - same as Android debug button
SAMPLE_FILE="${SAMPLE_FILE:-20260306_133927.m4a}"

if [[ -n "$1" ]]; then
  BASE_URL="$1"
else
  # From local.properties base.url (emulator uses 10.0.2.2; host uses 127.0.0.1)
  BASE_URL=$(grep -E '^base\.url=' "$PROJECT_ROOT/local.properties" 2>/dev/null | cut -d= -f2 || echo "https://127.0.0.1:8081")
fi

# For host machine, 10.0.2.2 is emulator-only; use 127.0.0.1
BASE_URL="${BASE_URL/10.0.2.2/127.0.0.1}"
BASE_URL="${BASE_URL%/}"

echo "=== Backend chat test ==="
echo "Base URL: $BASE_URL"
echo "Sample:   $SAMPLE_FILE"
echo ""

# 1. Health check
echo "1. GET /health"
HEALTH=$(curl -sk --connect-timeout 3 "$BASE_URL/health" 2>/dev/null || true)
if [[ -z "$HEALTH" ]]; then
  echo "   FAIL: Backend not reachable at $BASE_URL"
  exit 1
fi
echo "   OK: $HEALTH"

# 2. Dev token
echo ""
echo "2. GET /dev/token"
TOKEN_RESP=$(curl -sk --connect-timeout 3 "$BASE_URL/dev/token" 2>/dev/null || true)
if [[ "$TOKEN_RESP" != *"token"* ]]; then
  echo "   FAIL: Dev token not available (GYM_DEV_MODE=true?)"
  echo "   Response: $TOKEN_RESP"
  exit 1
fi
TOKEN=$(echo "$TOKEN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "   OK: token=$TOKEN"

# 3. Base64 encode sample audio
echo ""
echo "3. Encoding sample audio"
if [[ ! -f "$SAMPLES_DIR/$SAMPLE_FILE" ]]; then
  echo "   FAIL: Sample not found: $SAMPLES_DIR/$SAMPLE_FILE"
  exit 1
fi
AUDIO_B64=$(base64 -i "$SAMPLES_DIR/$SAMPLE_FILE" | tr -d '\n')
echo "   OK: $(echo -n "$AUDIO_B64" | wc -c) chars base64"

# 4. POST /chat
echo ""
echo "4. POST /chat (audio)"
CHAT_JSON=$(jq -n --arg audio "$AUDIO_B64" '{audio_base64: $audio, audio_format: "m4a"}')
CHAT_RESP=$(curl -sk --connect-timeout 30 -X POST "$BASE_URL/chat" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$CHAT_JSON" 2>/dev/null || true)

if [[ -z "$CHAT_RESP" ]]; then
  echo "   FAIL: No response from /chat"
  exit 1
fi

if echo "$CHAT_RESP" | grep -q '"error"'; then
  echo "   FAIL: API error"
  echo "$CHAT_RESP" | jq . 2>/dev/null || echo "$CHAT_RESP"
  exit 1
fi

echo "   OK: Response received"
echo ""
echo "Response:"
echo "$CHAT_RESP" | jq . 2>/dev/null || echo "$CHAT_RESP"
echo ""
echo "=== Backend chat test PASSED ==="
