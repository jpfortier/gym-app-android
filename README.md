# Gym App (Android)

Voice-first workout logging for Android. Backend: [gym-app](https://github.com/jpfortier/gym-app).

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Gradle (Kotlin DSL)
- Min SDK 24, Target SDK 34

## Setup

1. **Android SDK** — Install [Android Studio](https://developer.android.com/studio) (includes SDK) or the command-line tools.
2. **local.properties** — Create with `sdk.dir=/path/to/android-sdk` (e.g. `~/Library/Android/sdk` on macOS). Android Studio creates this when you open the project.
3. **Build:** `./gradlew assembleDebug`
4. **Run on device/emulator:** `./gradlew installDebug` (requires connected device or running emulator)
5. **Google Sign-In:** Add your OAuth web client ID to `app/build.gradle.kts` in `defaultConfig`:
   ```kotlin
   buildConfigField("String", "GOOGLE_CLIENT_ID_WEB", "\"YOUR_WEB_CLIENT_ID.apps.googleusercontent.com\"")
   ```
   Use the same client ID as the backend (`GYM_GOOGLE_CLIENT_ID`). Create an Android OAuth client in Google Cloud Console if needed.
6. Add `google-services.json` for FCM (when ready)

## Mobile MCP

For AI-driven app testing, add [mobile-mcp](https://github.com/mobile-next/mobile-mcp) to Cursor MCP settings. Requires Node.js v22+, Android SDK, and an emulator or USB-connected device.

## API

Backend runs on Fly.io. See [docs/backend-endpoints.md](docs/backend-endpoints.md) for the endpoint list. Full docs: [gym-app docs/api.md](https://github.com/jpfortier/gym-app/blob/main/docs/api.md).
