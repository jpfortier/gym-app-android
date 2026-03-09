# Gym App Android - Scratchpad

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
| **Voice** | Android SpeechRecognizer + backend Whisper fallback |
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

## Project Status Board

- [x] Stack plan documented
- [x] Mobile MCP installed in Cursor
- [x] Android project structure created
- [x] Base Compose app (builds when SDK present)
- [x] Base URL config (BuildConfig)
- [x] Technology foundation (deps, api, auth)
- [ ] First screen (chat/home)
- [ ] API client + auth wired to UI

## Executor's Feedback or Assistance Requests

- **Android SDK required:** Build succeeds only when `local.properties` has valid `sdk.dir` pointing to Android SDK.
- **Base URL:** Debug (emulator) = `https://10.0.2.2:8081`, Release = `https://gym-app.fly.dev`, Physical = `https://<mac-ip>:8081`. See `gym/docs/android-developer-reference.md`.
- **Mobile MCP:** Now working (full path + PATH env in mcp.json). Verified: app launches, screenshot captured app content (Gym App, API URL, Health error). CLEARTEXT fix applied.

## Lessons

- **Connections:** See `gym/docs/android-developer-reference.md`. Backend uses HTTPS (mkcert for local). No cleartext.
