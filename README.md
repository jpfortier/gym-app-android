# PR Tracks (Android)

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

Backend runs on Fly.io. See [gym-app docs/api.md](https://github.com/jpfortier/gym-app/blob/main/docs/api.md).

Local API reference: [docs/api-reference.md](docs/api-reference.md)

## Testing the backend

To verify the backend accepts audio before running the full cycle Android test:

```bash
# From project root. Backend must be running (make run in ../gym).
./scripts/test-backend-chat.sh
# Or with explicit URL:
./scripts/test-backend-chat.sh https://127.0.0.1:8081
```

Uses the same sample audio (`20260306_133927.m4a` — "Close grip bench 130") as the debug buttons. Requires `jq`.

## Full cycle test

`chatScreen_devSignIn_sendSample_receivesResponse` sends real audio to the backend and waits for the response. Requires:

1. Backend running at `base.url` (from `local.properties`), with `GYM_DEV_MODE=true`
2. Emulator reachable: if HTTPS fails (mkcert), run backend without TLS and use `base.url=http://10.0.2.2:8081`

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.gymapp.ChatScreenTest#chatScreen_devSignIn_sendSample_receivesResponse
```
