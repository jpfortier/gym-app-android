#!/bin/bash
# Build APK, generate version.json, upload both to R2.
# Requires: npx wrangler (logged in)
set -e
cd "$(dirname "$0")/.."
./gradlew assembleRelease writeVersionJson
npx wrangler r2 object put gym-app/apk/gym-app-release.apk --file app/build/outputs/apk/release/app-release-unsigned.apk --remote
npx wrangler r2 object put gym-app/version.json --file version.json --remote
echo "Uploaded APK and version.json to R2"
