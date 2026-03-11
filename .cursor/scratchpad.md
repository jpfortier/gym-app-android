# PR Tracks Android - Scratchpad

## Stack Plan

| Layer | Choice |
|-------|--------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose (Material 3) |
| **Build** | Gradle (Kotlin DSL) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 34 |
| **HTTP** | OkHttp + Retrofit (or Ktor client) |
| **Auth** | Google Sign-In (credential manager) |
| **Voice** | Record audio → POST to backend (Whisper). m4a, VAD. |
| **Testing** | Compose Testing (ui-test-junit4), JUnit 4 |
| **Automation** | Mobile MCP (for AI-driven app testing) |

---

## Technology Foundation Plan (Planner)

**Goal:** Add all dependencies and package structure needed to support the full app. No screens yet—just the plumbing.

### 1. Dependencies to Add

| Category | Library | Purpose |
|----------|---------|---------|
| **HTTP** | `retrofit2:retrofit` | REST client |
| **HTTP** | `retrofit2:converter-gson` | JSON ↔ Kotlin (or `converter-moshi`) |
| **HTTP** | `okhttp3:okhttp` | HTTP client (Retrofit uses it) |
| **HTTP** | `okhttp3:logging-interceptor` | Debug logging (optional) |
| **Auth** | `com.google.android.gms:play-services-auth` | Google Sign-In |
| **Auth** | `androidx.credentials:credentials` | Credential Manager (modern) |
| **Auth** | `androidx.credentials:credentials-play-services-auth` | Google ID via Credential Manager |
| **JSON** | `com.google.code.gson:gson` | JSON serialization (or kotlinx.serialization) |
| **Images** | `io.coil-kt:coil-compose` | Load PR images from presigned URLs |
| **Coroutines** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Async (often transitive) |
| **Lifecycle** | `androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModel for Compose |
| **Lifecycle** | `androidx.lifecycle:lifecycle-runtime-compose` | collectAsStateWithLifecycle |

**Note:** Credential Manager is the modern path; `play-services-auth` is the older one. Pick one. Credential Manager is recommended for new apps.

### 2. Package Structure

```
dev.gymapp/
├── api/                    # HTTP layer
│   ├── GymApi.kt           # Retrofit interface
│   ├── models/             # Request/response DTOs
│   │   ├── ChatRequest.kt
│   │   ├── ChatResponse.kt
│   │   ├── User.kt
│   │   └── ...
│   └── ApiClient.kt        # Retrofit instance + OkHttp with auth interceptor
├── auth/                   # Google Sign-In
│   ├── AuthRepository.kt   # Get ID token, sign out
│   └── (Credential Manager or play-services-auth usage)
├── ui/                     # (screens later)
│   └── theme/
└── MainActivity.kt
```

### 3. Build Configuration

- **Retrofit base URL:** Use `BuildConfig.BASE_URL` (already in place).
- **Auth interceptor:** OkHttp interceptor that adds `Authorization: Bearer <token>` to every request. Token from AuthRepository.
- **Error handling:** Retrofit `Response` / `Call` error body → parse `{ error, code, error_token }` for display.

### 4. Auth Flow (High-Level)

1. App starts → check if user has valid token (or cached credential).
2. If not → show sign-in (Credential Manager / Google Sign-In).
3. On success → store token (in-memory for v1; optionally EncryptedSharedPreferences for persistence).
4. All API calls → interceptor attaches token.
5. On 401 → clear token, redirect to sign-in.

### 5. What NOT to Add Yet

- Hilt/Koin (manual injection or singleton is fine for v1).
- Room/local DB (API-only for v1).
- Navigation library (single Activity for now).
- Voice/SpeechRecognizer (add when building chat UI).

### 6. Implementation Order

1. Add all Gradle dependencies.
2. Create `api/models/` with DTOs for chat, user, error.
3. Create `GymApi` Retrofit interface (GET /me, POST /chat, GET /health).
4. Create `ApiClient` with OkHttp + auth interceptor (token placeholder for now).
5. Create `auth/` package and AuthRepository (Google Sign-In or Credential Manager).
6. Wire auth token into ApiClient interceptor.

**Success criteria:** Can call GET /health (no auth) and GET /me (with token) from a test or placeholder code. Auth flow completes and provides token.

**Done:** Technology foundation implemented. MainActivity calls GET /health on launch and displays result. Auth ready (needs GOOGLE_CLIENT_ID_WEB in build.gradle).

---

## Full App Implementation Plan (Mar 2026)

**Source:** Q&A with JP. Reference: `gym/docs/android-developer-reference.md`.

### Decisions Summary

| Area | Decision |
|------|----------|
| **Sign-in screen** | Logo (logo.png) + "Sign in with Google" button only |
| **Post sign-in** | Chat screen first |
| **Chat input** | Voice primary (mic); keyboard icon in bottom corner to switch to text mode |
| **Chat layout** | Conversation history: user right + bubbles, software left + no bubbles |
| **Navigation** | Bottom-left icon → Dashboard |
| **Dashboard** | Scrollable: Latest PR + Streak tiles; Recent PRs (last 3 per type); Exercise type cards; Activity timeline. Refresh button. |
| **Errors** | Snackbar; on tap show full debug info (error, code, error_token) for dev |
| **401 handling** | Try refresh silently; if fails, redirect to sign-in with "signed out" message |
| **Voice** | Record audio, POST to backend (no SpeechRecognizer). m4a format. VAD auto-stop after 1.5s silence (fixed, easy to change in code) |

### High-Level Task Breakdown

1. **Sign-in screen**
   - Add logo.png to drawable. Screen: logo + "Sign in with Google" button.
   - On success → navigate to Chat.
   - Support "signed out" message when redirected after 401.

2. **Auth + 401 flow**
   - On 401: attempt token refresh (Credential Manager). If fails → sign out, navigate to sign-in with message.
   - Add interceptor or callback for 401 handling.

3. **Chat screen (main)**
   - Conversation list: user messages right + bubbles, software left + no bubbles.
   - Input bar: mic (default) + keyboard icon to switch modes.
   - Text mode: text field, send button.
   - Voice mode: mic button, record → VAD stop (1.5s silence) → POST audio_base64 (m4a).
   - Handle all chat intents: log, query, correction, remove, restore, note, unknown. Render response appropriately.

4. **Audio recording**
   - Record to m4a (AAC). Implement VAD with 1.5s silence threshold.
   - Encode to base64, POST to /chat with audio_format: "m4a".

5. **Dashboard**
   - Bottom-left icon on chat opens Dashboard.
   - 2×2 grid: Latest PR (real, from GET /prs), 3 placeholder tiles.
   - Display-only.

6. **Error handling**
   - Snackbar on API failure. On tap: expand/show error, code, error_token.

7. **Navigation**
   - Single Activity, Compose navigation: SignIn ↔ Chat ↔ Dashboard.
   - Chat is default after sign-in. Dashboard via bottom-left icon.

### Success Criteria (per task)

- **Sign-in:** Logo visible, button triggers Google Sign-In, on success → Chat.
- **401 flow:** 401 triggers refresh attempt; on failure, sign out and show message on sign-in.
- **Chat:** Messages display correctly, text send works, voice record+send works.
- **Dashboard:** Opens from icon, shows Latest PR + 3 placeholders.
- **Errors:** Snackbar appears; tap shows debug details.

---

## Project Status Board

- [x] Stack plan documented
- [x] Mobile MCP installed in Cursor
- [x] Android project structure created
- [x] Base Compose app (builds when SDK present)
- [x] Base URL config (BuildConfig)
- [x] Technology foundation (deps, api, auth)
- [x] Sign-in screen (logo + Google button)
- [x] Auth + 401 flow (refresh, redirect with message)
- [x] Chat screen (conversation, text + voice input)
- [x] Audio recording (m4a, VAD 1.5s)
- [x] Dashboard (2×2 grid, Latest PR + placeholders)
- [x] Dashboard expansion (Streak, Recent PRs by type, Exercise cards, Activity timeline, Refresh)
- [x] Error handling (snackbar + debug on tap)

## Executor's Feedback or Assistance Requests

- **Chat UI and Voice Flow (Mar 2026):** Implemented: (1) Removed typing mode—mic only. (2) Mic as floating icon in circle (FAB) above bottom. (3) Dashboard icon in top-left with border. (4) Chat history on load via GET /chat/messages. (5) Voice transcription: placeholder until reply, reload history after send. (6) Server-side audio already done. (7) GET /chat/history added to docs. (8) README links to docs/api-reference.md. GymApi.chatMessages() added; ChatMessagesResponse DTO; ChatViewModel loads on init and reloads after sendAudio success.
- **Phase 6 complete:** Error handling implemented. Snackbar on API failure; tap "Details" shows dialog with error, code, error_token. ChatViewModel and DashboardViewModel parse ApiError via ErrorBodyParser.
- **Dev mode:** GET /dev/token (api.md) returns `dev:<email>`. Debug-only "Dev sign-in" button on SignInScreen. Instrumentation tests use dev token. Debug sample buttons (m4a from gym/samples/audio) send audio to chat. Tests: signOut() in @Before so SignIn tests see sign-in screen.
- **Android SDK required:** Build succeeds only when `local.properties` has valid `sdk.dir` pointing to Android SDK.
- **Base URL:** Debug (emulator) = `https://10.0.2.2:8081`, Release = `https://gym-app.fly.dev`, Physical = `https://<mac-ip>:8081`. See `gym/docs/android-developer-reference.md`.
- **Mobile MCP:** Now working (full path + PATH env in mcp.json). Verified: app launches, screenshot captured app content (PR Tracks, API URL, Health error). CLEARTEXT fix applied.

## Lessons

- **Connections:** See `gym/docs/android-developer-reference.md`. Backend uses HTTPS (mkcert for local). No cleartext.
- **Instrumentation tests:** (1) AuthRepository: make CredentialManager lazy + nullable so emulator without GMS doesn't crash. (2) Espresso 3.5.1 fails on API 36 with InputManager.getInstance NoSuchMethodException; upgrade to espresso-core 3.7.0. (3) Add @RunWith(AndroidJUnit4::class), testInstrumentationRunner, animationsDisabled.
- **Detekt:** config/detekt.yml with ignoreAnnotated for Composable/Test, MagicNumber excludes for theme/tests, LongParameterList functionThreshold 7.
- **Markdown for chat:** Assistant messages use `MarkdownText` (jeziellago compose-markdown 0.6.0). User messages stay plain `Text`. boswelja/compose-markdown requires minSdk 28; jeziellago works with minSdk 24.
- **ChatScreenTest mkcert:** Test sends sample audio to backend. If it times out waiting for "Voice message", emulator may not trust mkcert for 10.0.2.2. Verified: app's mkcert_root_ca.pem matches system CA; backend cert has 10.0.2.2 in SAN. Workaround: run backend without TLS, use base.url=http://10.0.2.2:8081.
- **Composable tests:** Use `ComposeTestActivity` (empty activity in manifest) with `createAndroidComposeRule<ComposeTestActivity>()` and `setContent` for composable-only tests. MainActivity already sets content so cannot use setContent on it.
- **Backend test:** `scripts/test-backend-chat.sh` tests POST /chat with sample audio from host. Full cycle Android test needs emulator→host reachability; use `base.url=http://10.0.2.2:8081` if HTTPS/mkcert fails from emulator.
- **ChatScreenComponents haptic:** LocalHapticFeedback is in `androidx.compose.ui.platform`; HapticFeedbackType.Confirm may be absent in some Compose BOM versions—use LongPress instead.
