#!/usr/bin/env bash
set -euo pipefail

# Build android variants with two different NOTHING_KEY values.
# - Debug uses manifest placeholder already set to "test" in Gradle.
# - Release will be built twice by injecting NOTHING_KEY via -PNOTHING_KEY.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

REAL_KEY_DEFAULT="1897db6d0bfd45318a321a207e3bdbcd"
TEST_KEY_DEFAULT="test"

# Allow overrides via environment variables
REAL_KEY="${NOTHING_KEY_REAL:-$REAL_KEY_DEFAULT}"
TEST_KEY="${NOTHING_KEY_TEST:-$TEST_KEY_DEFAULT}"

GRADLEW="${ROOT_DIR}/gradlew"

echo "==> Cleaning previous builds"
"$GRADLEW" clean -q | cat

echo "==> Building Debug (uses test key via manifest placeholder)"
"$GRADLEW" :androidApp:assembleGoogleDebug -x lint -q | cat

OUTPUT_DIR="${ROOT_DIR}/androidApp/build/outputs/apk"
ARTIFACTS_DIR="${ROOT_DIR}/artifacts"
mkdir -p "$ARTIFACTS_DIR"

dbg_apk=$(ls -1 "$OUTPUT_DIR"/google/debug/*.apk 2>/dev/null | head -n1 || true)
if [[ -n "${dbg_apk}" ]]; then
  cp -f "${dbg_apk}" "${ARTIFACTS_DIR}/app-google-debug-testKey.apk"
  echo "Saved: ${ARTIFACTS_DIR}/app-google-debug-testKey.apk"
else
  echo "Warning: Debug APK not found."
fi

echo "==> Building Release with TEST key"
"$GRADLEW" :androidApp:assembleGoogleRelease -PNOTHING_KEY="${TEST_KEY}" -x lint -q | cat
test_apk=$(ls -1 "$OUTPUT_DIR"/google/release/*.apk 2>/dev/null | head -n1 || true)
if [[ -n "${test_apk}" ]]; then
  cp -f "${test_apk}" "${ARTIFACTS_DIR}/app-google-release-testKey.apk"
  echo "Saved: ${ARTIFACTS_DIR}/app-google-release-testKey.apk"
else
  echo "Warning: Release APK (test key) not found."
fi

echo "==> Building Release with REAL key"
"$GRADLEW" :androidApp:assembleGoogleRelease -PNOTHING_KEY="${REAL_KEY}" -x lint -q | cat
real_apk=$(ls -1 "$OUTPUT_DIR"/google/release/*.apk 2>/dev/null | head -n1 || true)
if [[ -n "${real_apk}" ]]; then
  cp -f "${real_apk}" "${ARTIFACTS_DIR}/app-google-release-realKey.apk"
  echo "Saved: ${ARTIFACTS_DIR}/app-google-release-realKey.apk"
else
  echo "Warning: Release APK (real key) not found."
fi

echo "==> Done. Artifacts in: ${ARTIFACTS_DIR}"

