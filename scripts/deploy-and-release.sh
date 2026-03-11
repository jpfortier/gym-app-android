#!/bin/bash
# Deploy to R2, commit version bump, push.
# Run this instead of upload-apk.sh when you want a clean deploy with no orphan version.
# Usage: ./scripts/deploy-and-release.sh
set -e
cd "$(dirname "$0")/.."

echo "1. Deploying to R2 (increment, build, upload)..."
./scripts/upload-apk.sh

echo ""
echo "2. Committing version bump..."
git add app/build.gradle.kts
git diff --cached --quiet && { echo "No version change to commit."; exit 0; }
VERSION=$(grep 'versionName = ' app/build.gradle.kts | head -1 | sed 's/.*"\([0-9.]*\)".*/\1/')
git commit -m "chore: bump version to $VERSION"

echo ""
echo "3. Pushing to origin..."
git push

echo ""
echo "Done. Version $VERSION is on R2 and pushed to origin."
