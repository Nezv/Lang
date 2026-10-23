# Changelog

All notable changes are recorded here, together with the architecture and design decisions behind them.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow SemVer; `0.x` while in beta.

## Delivery phases

The four quality goals are tackled in order, grouped into two beta phases:

| Phase | Goals, in order | Exit criteria |
|---|---|---|
| **Beta 1** (`0.1` → `0.9`) | 1. **Functionality**: every planned feature works end to end. 2. **Reliability**: it keeps working (errors handled, data never lost, automated tests, CI). | The owner signs off on the GUI and features on both platforms; no known data-loss bugs. |
| **Beta 2** (`0.10` → `1.0`) | 3. **Performance**: startup, memory, smooth scrolling, app size. 4. **Security**: secrets, privacy, signed releases. | Store-ready builds for Android and Windows. |

---

## [Unreleased]

### Added
- **Tutor docs** (`docs/tutor/`): notes from the first chat tutor session, a retrospective of it, the learner profile,
  and a draft **harness spec** for the app's tutor (learner model, cache-friendly prompt, turn schema with
  per-fragment corrections, deterministic vocabulary validator, correction-card UI, evals).
- **`CLAUDE.md`** and a project **`/tutor` skill** (`.claude/skills/tutor/`) so chat sessions start with the learner's
  level, vocabulary and format, and save notes at the end.
- The `/tutor` skill also runs **Japanese** chat sessions (`/tutor ja`): Japanese, romaji and English in each cell,
  notes in `docs/tutor/sessions/YYYY-MM-DD-japanese.md`. The app itself stays Chinese-only.

### Decisions
- The chat tutor is the prototype of the app tutor: rules that work there move into the app's prompt and validator.
- Lesson from the first session: the tutor's main failures were missing context and pacing, not model ability. The
  harness, not the model, is where to fix them.
- The `/tutor` skill reads the known vocabulary from the Anki project next to this repo (`../Anki/practice/vocab.md`,
  Immersive Chinese lessons 1–79, and `focus.md`, Anki's struggling/due words) instead of a copy here. This repo is
  public, and the word list is course content. Chat sessions run on the owner's Claude subscription, with no API key.
- The skill commits session notes but no longer pushes them: they hold personal facts, and the repo is public.
- One `/tutor` skill and one learner profile for both languages. Preferences and personal facts are shared (a fact
  told in Chinese is a topic in Japanese too); level, error patterns, words and the open question are kept per
  language. A table in the skill holds what differs: reading line, notes file, word source, typing slips.
- Japanese has no word list (no Anki deck), so its tutor rows stay with common JLPT N5 words and recycle the profile's
  Japanese "seen once" words.
- New rule from the 2026-10-21 and 2026-10-22 sessions, where the tutor asked again for what the learner had just
  said: never ask for something the learner has already told the tutor. It is in the skill and the harness spec.

---

## [0.2.0] — 2026-10-15 — AI tutor (Beta 1, functionality)

### Added
- **`ClaudeTutorEngine`** (`shared/commonMain`): free-form role-play through the Claude Messages API over the shared
  Ktor client. Each reply is structured: hanzi, pinyin with tone marks, English translation, two suggested learner
  replies, an optional correction of the learner's last message, and a `finished` flag. They map onto `ChatMessage`
  (new `correction` field), `Phrase` and `TutorTurn`.
- **`FallbackTutorEngine`**: uses the AI when a key is set and falls back to `ScriptedTutorEngine` when there is no
  key, no network or the API fails. `TutorMode` (AI online / no key / unreachable) feeds a status line in both GUIs.
- **Status hint** in the Windows tutor side panel and under the scenario description on Android. Corrections show as a
  💡 note inside the tutor's chat bubble (shared `ChatBubble`).
- **Key setup** for private beta builds (README): `ANTHROPIC_API_KEY` environment variable or a gitignored
  `secrets.properties`. Optional `LANG_TUTOR_MODEL` / `tutor.model` overrides the model.
- Ktor `MockEngine` tests for request building, response parsing, refusals, cut-offs and the fallback behaviour.
- HTTP timeouts in the client factory (10 s connect, 60 s request).

### Changed
- App version 0.2.0 (Android `versionName`, Windows `packageVersion`). Android now generates `BuildConfig` fields for the key.

### Verification results
`gradlew :shared:allTests :androidApp:assembleDebug :desktopApp:compileKotlin` passes. The live API was **not** called in
this session (no key available here), so the conversation quality in each scenario still needs a check with a real key.

### Architecture and design decisions

#### ADR-015: AI tutor: Claude Messages API over raw HTTP, structured output, scripted fallback
- **Raw HTTP, not an SDK.** The Anthropic SDKs for the JVM do not work in Kotlin Multiplatform `commonMain` (and the
  Android SDK path would duplicate the Windows one). `shared` already has Ktor with OkHttp (Android) and the JDK
  engine (Windows), so one `ClaudeTutorEngine` serves both apps. Request shape was taken from the `claude-api` skill,
  not from memory.
- **Model `claude-opus-5-5`** (the current Opus and the skill's default). Cost per turn is small (short prompts and
  replies); changing to a cheaper model is a one-line setting (`LANG_TUTOR_MODEL`), a deliberate owner decision, not
  made silently. `effort: "low"` because the tutor speaks one or two short sentences; thinking cannot be disabled on
  this model, so effort is the control. Not streamed: replies are ~150 tokens, and the GUI shows a typing bubble.
- **Structured output** via `output_config.format` with a JSON schema (`additionalProperties: false`, all fields
  required). The model can't return free text the GUI cannot show. `correction` is a required string that is empty
  when there is nothing to fix (simpler for the schema than a nullable). Only two hints are kept; none on a finished turn.
- **Prompt**: the system prompt is built from the scenario (persona, description) and fixes HSK 1–2 vocabulary, one or
  two short sentences, pinyin with tone marks, stay in scenario, gently correct, two suggested replies, set
  `finished` at a natural ending. The history is sent as plain user/assistant text; a stage cue is the first user
  message because the API requires the first message to be from the user.
- **Refusal handling**: requests carry `fallbacks: "default"` (beta header `server-side-fallback-2026-07-01`) as the
  skill recommends, so a safety-classifier decline is retried server-side. A `refusal` or `max_tokens` stop reason,
  malformed JSON or any HTTP/network error becomes an exception and triggers the offline fallback.
- **Fallback policy**: no key → scripted for the whole session (`NoKey`). AI failure on a restart → scripted for that
  conversation. AI failure mid-conversation → that conversation restarts on the script (the script has its own
  progress, so continuing it midway would be wrong) and the status shows "unreachable". Restarting a scenario tries
  the AI again. No automatic retry within a turn, so the learner is not left waiting.
- **Key handling (private beta only)**: never in the repo. Windows reads the environment variable, then
  `secrets.properties` in the working directory, then `%APPDATA%\Lang\secrets.properties`. Android reads the same
  environment variable or the repo-root `secrets.properties` **at build time** into a `BuildConfig` field, so the key
  exists in that APK but not in source. Anyone who has the APK can extract the key, so do not share these builds.
  Real protection (backend proxy or per-user keys, Keystore/DPAPI) is Beta 2 work, already in TODO.
- **Tests** use Ktor `MockEngine`; the request body is sent as a JSON string so it does not depend on the client's
  ContentNegotiation setup.

---

## [0.1.0] — 2026-10-15 — Environment and skeleton (Beta 1 begins)

### Added
- **Toolchain**, installed per user without admin or reboot: Temurin JDK 21.0.12.1, Android SDK (platform 37,
  build-tools 37.0.0 and 36.0.0, platform-tools 37.0.1, emulator 37.2.12, Android CLI), Android 16 emulator image
  with Google Play, a `medium_phone` emulator, and Gradle 9.8.1 through the wrapper. See ADR-002.
- **Project structure**: four Gradle modules, `shared`, `designsystem`, `androidApp` and `desktopApp` (ADR-003).
- **Shared logic** (`shared`):
  - Content model: `Lesson` → `Sentence` → `Token`, each token with hanzi, per-syllable pinyin and an English gloss.
  - 3 starter lessons, 15 HSK 1 sentences: greetings, getting to know someone, at the café.
  - FSRS-6 spaced-repetition engine and an Anki-style scheduler with learning and relearning steps (ADR-006).
  - SQLite database for cards and the review log, via SQLDelight (ADR-007).
  - Tutor engine interface plus an offline scripted tutor with 2 role-plays (café order, meeting your teacher),
    reply hints, a "try in Chinese" nudge and re-prompts for off-topic answers (ADR-009).
  - Speech interface. Android uses the system text-to-speech; Windows uses System.Speech (ADR-010).
  - ViewModels for Today, Learn, Review and Tutor, shared by both GUIs (ADR-004).
  - HTTP client factory (Ktor) for the coming AI tutor.
- **Shared look** (`designsystem`): brand colours (light and dark), tone colours, `SentenceText` (pinyin above
  each character, tappable words), `WordMeaning`, `ChatBubble`, `TypingBubble`, answer-button colours.
- **Android GUI** (`androidApp`): bottom navigation with a due-count badge; Today dashboard; lesson list; full-screen
  swipeable lesson player; tap-to-reveal review with four answer buttons showing the next interval; tutor chat with
  scenario chips, reply chips and a text box (ADR-005).
- **Windows GUI** (`desktopApp`): sidebar navigation with keyboard shortcuts; dashboard; two-pane lesson browser;
  keyboard-driven review (Space, 1–4, R); three-column tutor view with scenario list, display switches and reply
  cards; off-screen screenshot tool (ADR-005).
- **Tests**: 17 unit tests covering the scheduler, pinyin, content integrity, interval labels, the tutor script and
  the database repository. Shared tests run on both the desktop JVM and the Android host.
- **Launchers**: `run-desktop.cmd` and `run-android.cmd` for one-click runs.
- **Docs**: README, this changelog, TODO.

### Fixed during verification (before release of 0.1.0)
- **Packaged Windows app would not start** ("Failed to launch JVM"). The trimmed Java runtime that jpackage bundles
  lacked modules the app uses. It now declares `java.sql` (SQLite), `java.net.http` and `jdk.crypto.ec` (HTTPS for
  the AI tutor), plus `java.instrument`, `java.management` and `jdk.unsupported` as suggested by
  `suggestRuntimeModules`.
- **Windows dark mode:** sidebar text was invisible because it was drawn on a plain background instead of a
  `Surface`, so it didn't pick up the theme's text colour.
- **Small Windows window:** the lesson buttons were squeezed. Controls and toggles now wrap onto extra rows, and the
  minimum window size is 1024 × 700.
- **Stale Review badge:** counts now refresh on every navigation, not only on the Today screen.
- **Review card text:** it no longer says "save sentences" when the deck has cards that simply aren't due yet.

### Verification results
- Unit tests: 17/17 pass on the desktop JVM; the 15 shared ones also pass as Android host tests.
- Android: the APK was installed on the API 36 emulator. Every tab was exercised: save a sentence, tap a word,
  review it, chat with the tutor. Light and dark themes checked; data survives a reinstall. Cold start of the debug
  build on the emulator: 4.5–7.8 s, to be optimised in Beta 2.
- Windows: the packaged `Lang.exe` launched as a real window (dark theme follows Windows) and correctly reported
  "No Chinese voice installed". Peak memory about 250 MB. Off-screen renders cover every screen, dark mode and the
  smallest window size.
- Screenshots are in `docs/screenshots/`.

### Known limitations
- The tutor is scripted. It recognises keywords, not meaning, until the AI engine lands.
- This PC has no Chinese voice, so Listen is silent on Windows (the GUI says so). Voice input is not built yet.
- Learning progress is defined as "sentences saved to review". There are no streaks, statistics or settings yet.
- The app name (`Lang`), application id (`app.lang`) and launcher icon are placeholders.
- In dark mode the Windows title bar stays light (a limitation of Java windows; fixable with a DWM call or a custom
  title bar).
- The build prints one Gradle deprecation (`Configuration.setVisible`). It comes from a third-party plugin, not
  this project, and is harmless until Gradle 11.
- On first use, `adb.exe` (the Android emulator bridge) triggers a Windows Firewall prompt. adb only needs local
  connections, so the prompt can be cancelled.

### Architecture and design decisions

#### ADR-001: Cross-platform stack is Kotlin Multiplatform + Compose
**Context.** One app for Windows and Android, sharing its source code but with a different GUI on each platform.
Hard constraints on this PC: no administrator rights, no restart, no installers. Visual Studio, an Android SDK and a
JDK were all missing. Git, Node 24, Python 3.12, the .NET 10 runtime (no SDK), and WinGet were present, and Windows
Hypervisor Platform was enabled.

**Options evaluated** (each one checked against this machine):

| Option | Windows build needs | Android build needs | Verdict |
|---|---|---|---|
| **Kotlin Multiplatform + Compose** | JDK only (Compose Desktop runs on the JVM) | JDK + Android SDK | ✅ **Chosen.** Every part installs portably. Shared logic plus native Android UI is Google's own recommended approach. |
| Flutter | Visual Studio 2022 with the C++ workload (admin install) | Android SDK | ❌ Windows target blocked. |
| React Native (+ RN Windows) | Visual Studio 2022 with C++/UWP workloads | Android SDK | ❌ Windows target blocked. |
| Tauri 2 (Rust + web UI) | MSVC linker (VS Build Tools, admin); the GNU toolchain route is fragile | NDK + Rust targets | ❌ Blocked or fragile. |
| .NET MAUI / Avalonia (C#) | .NET SDK (per-user install possible) | Android workload + JDK + SDK | ⚠️ Feasible. Weaker native Android UI story, MAUI quality concerns, smaller ecosystem for Android-first apps. |
| Electron + Capacitor (TypeScript) | Node (present) | JDK + SDK + Gradle | ⚠️ Feasible. Two web views: heavier on memory and battery, less native feel. Its JS libraries (hanzi-writer, ts-fsrs) were the main pull. |

**Decision.** Kotlin Multiplatform (KMP) with Compose. One language (Kotlin) for everything. Business logic is
compiled for both platforms. The GUI is written per platform with the same toolkit: Jetpack Compose on Android,
Compose Multiplatform for desktop on Windows.

**Consequences.**
- (+) Native Android UI. The Windows app is a real desktop app that can be packaged as MSI or EXE.
- (+) Toolchain needs only a JDK and an Android SDK.
- (−) The Windows app runs on a bundled JVM. Startup is about 1–2 s and memory about 200 MB; handled in Beta 2.
- (−) The Windows look is Compose/Material rather than native WinUI. It can be restyled; see ADR-005.
- Not used: Flutter, React Native, Tauri, .NET. They stay as options if the constraints change.

#### ADR-002: Portable, per-user toolchain
**Decision.** Install everything into the user profile from official archives, verified by checksum:
- JDK: Eclipse Temurin 21.0.12.1 LTS zip (SHA-256 checked) into `%USERPROFILE%\.jdks\`, the folder
  IntelliJ and Android Studio scan automatically.
- Android SDK: command-line tools zip (SHA-1 checked against Google's repository manifest) into the standard
  `%LOCALAPPDATA%\Android\Sdk`. Packages were then installed with the new **Android CLI** (`android sdk install`,
  which replaces the deprecated `sdkmanager`): platform 37, build-tools 37.0.0, platform-tools, emulator, and an
  API 37 Google Play x86_64 image. Accepting the licenses installed AGP's default build-tools 36.0.0 automatically.
- Emulator: created with `android emulator create medium_phone`, which picked the API 36 Google Play image.
  `emulator -accel-check` confirms WHPX acceleration works without admin.
- Gradle 9.8.1 via the wrapper with a pinned SHA-256 (`gradle-wrapper.properties`). The wrapper was generated from
  a Gradle 9.4.1 copy already in the cache, in a temp folder, so no global Gradle install was needed.
- User environment variables `JAVA_HOME`, `ANDROID_HOME` and `PATH` were set (no admin needed);
  `local.properties` points Gradle at the SDK.

**Why not WinGet, Chocolatey or Scoop?** Most JDK/SDK packages install machine-wide (UAC prompt), and Scoop was
not installed. Zip archives are reproducible and easy to remove.

The Gradle cache already held Temurin 21/25 JDKs from Gradle's toolchain download. They were not reused: Gradle may
prune that folder, and the IDEs don't look there.

#### ADR-003: Module layout — `shared` / `designsystem` / `androidApp` / `desktopApp`
**Decision.**
- `shared` (KMP library, targets `android` and `jvm("desktop")`): domain model, content, SRS engine, database,
  tutor, speech interface, ViewModels. No UI code.
- `designsystem` (KMP + Compose): theme tokens and a few Chinese-specific widgets. Nothing about screens or navigation.
- `androidApp` (Android application) and `desktopApp` (JVM application): each owns its screens, navigation, window
  or activity, and platform wiring.

**Why.** It matches the requirement "shared source code, different GUI". It's also the layout that AGP 9 requires
anyway: the Android application must be a separate module from KMP code, and KMP libraries use the new
`com.android.kotlin.multiplatform.library` plugin.

**Planned split.** Once `shared` grows, split it into `core:*` and `feature:*` modules for build speed and clear
ownership. Not done yet; there's no benefit at this size.

#### ADR-004: What is shared and what is platform-specific
**Decision.**
- **Shared:** everything except drawing. Business rules, data, *and* screen logic: AndroidX `ViewModel`s with
  `StateFlow` state, built on the multiplatform `lifecycle-viewmodel`. Each GUI only renders a state and calls
  ViewModel functions such as `grade(Rating.Good)`.
- **Platform:** composables and layout, navigation, input (touch vs keyboard shortcuts), the database driver
  (`AndroidSqliteDriver` vs `JdbcSqliteDriver`), the speech engine, file locations, and app lifecycle.
- Platform pieces are injected through `createAppGraph(...)` (in `androidMain` and `desktopMain`) instead of
  scattering `expect`/`actual` declarations. That keeps the contract explicit and lets tests swap any piece.

**Consequences.** A behaviour fix lands on both platforms at once. GUIs can be redesigned without touching logic.
On Android, ViewModels survive screen rotation (`viewModel {}`). On Windows they live as long as the window
(`DesktopViewModels`).

#### ADR-005: Two GUIs designed for their platform
**Android (phone-first):** a bottom navigation bar with 4 tabs (Today, Learn, Review, Tutor), where Review shows a
due-count badge. The lesson player is full-screen, with sentences on a **swipeable pager**. Large controls sit in the
thumb zone, with tap-to-reveal cards and 64 dp answer buttons. Edge-to-edge layout, and the keyboard pushes the
tutor input up (`imePadding`, `adjustResize`).

**Windows (desktop-first):** a permanent **sidebar** with labels, hanzi and shortcut hints (`Ctrl+1…4`), plus a
voice-status note. The content sits in a rounded surface, Windows 11 style.
- **Lessons:** two panes, with lessons and sentences on the left and a large player (52 sp hanzi) on the right.
- **Review:** Anki-desktop keys (`Space` reveal / Good, `1–4` answer, `R` replay), shown on the buttons.
- **Tutor:** three areas: chat, side panel with scenarios, pinyin/English switches and clickable reply cards.
  `Enter` sends, `Shift+Enter` adds a new line.

Keyboard handling goes through one window-level `ShortcutRouter`. Each pane registers its own keys while visible,
so typing in the tutor box never triggers review keys.

**Shared look:** one brand palette, seal red, jade and imperial gold on warm paper, in light and dark modes.
Pleco-style tone colours (1 red, 2 green, 3 blue, 4 purple) are used on both platforms for recognition.
Android does not use wallpaper-based dynamic colour, to keep a consistent brand.

**Open option:** a Fluent (WinUI-like) skin for Windows, such as a Compose Fluent library, can be evaluated during
the Beta 1 GUI review. Screens would not change, only the components.

#### ADR-006: Spaced repetition is FSRS-6 with Anki-style steps
**Decision.** Port FSRS-6, the default scheduler in Anki 23.10+, from the reference `py-fsrs` 6.x implementation.
It uses the 21 default parameters and 90 % desired retention, with learning steps 1 m / 10 m, a relearning step of
10 m, a 100-year maximum interval, and short-term stability for same-day reviews. The four buttons and their
interval previews ("1m · 6m · 10m · 8d") match Anki, so Anki users feel at home.
**Why not SM-2?** FSRS needs fewer reviews for the same retention and is now Anki's standard. Every answer is
stored in `review_log` so per-user parameter optimisation can be added later.
**Deferred:** interval fuzz, daily new/review limits, parameter optimiser, load balancing. Learning-step cards come
back in the same session if due within 20 minutes, like Anki's learn-ahead.

#### ADR-007: Local database is SQLDelight on SQLite
**Decision.** SQLDelight 2.4: the SQL schema and queries live in `Card.sq`, and Kotlin code is generated from them.
Drivers: the Android SQLite driver, and on Windows the JDBC sqlite driver (bundled native library) with the database
at `%APPDATA%\Lang\lang.db`. Times are UTC epoch milliseconds.
**Why not Room KMP?** Both work. SQLDelight was chosen because (1) the SQL is explicit, which suits Anki-style data
and a future `.apkg` import (Anki files are SQLite databases); (2) it needs no KSP annotation processor, so there's
one less tool tied to Kotlin versions; (3) it has a long KMP track record.
**Deferred to Reliability:** schema migrations (`.sqm` files) before the first schema change ships, plus backups
and export.

#### ADR-008: Content model is tokenised sentences
**Decision.** A sentence is a list of tokens (word or punctuation). Each word holds its hanzi, pinyin with **one
syllable per character** (space-separated, with tone marks) and a short gloss. This enables pinyin above each
character (ruby), tone colouring, tap-to-translate and future word-level cards. Erhua words (哪儿 → nǎr) are
detected and shown with word-level pinyin. Pinyin follows dictionary tones; only 不 and 一 show tone sandhi, as in
most textbooks. Chinese text carries the `zh-CN` locale, so fonts render Simplified glyph shapes even on an English
system.
**Now:** content is Kotlin code (`SeedContent`), and a test checks its integrity. **Next:** JSON content packs with
audio, which can be downloaded or added without app updates.

#### ADR-009: The AI tutor sits behind an interface, with an offline engine first
**Decision.** `TutorEngine` (`opening`, `reply`, `scenarios`) hides how replies are produced. Version 0.1 ships
`ScriptedTutorEngine`, which steps through fixed role-plays with keyword checks, hints and re-prompts. With it the
whole conversation GUI could be built and tested without network or API keys, and it stays as the offline fallback.
The real engine will call an LLM (Anthropic Claude planned) through the shared Ktor client. A ViewModel cancels
stale replies when the scenario changes.
**Security note, deliberately deferred to Beta 2:** a client-side API key is acceptable only for private beta
builds. Releases need key protection or a small backend proxy.

#### ADR-010: Speech is platform engines behind `SpeechService`
**Decision.**
- **Android:** the system `TextToSpeech` engine set to `zh-CN`, with a `<queries>` manifest entry so Android 11+
  can see it. Status is reported (ready / missing voice / unavailable) and shown in the UI.
- **Windows:** the built-in `System.Speech` API, run through `powershell.exe`. It needs no dependencies, but has
  about 0.5 s of latency and needs a Chinese voice installed. **This PC has none** (only English voices). The app
  detects that and tells the user how to add one. The text is passed through an environment variable, never pasted
  into the command, so it can't inject commands.
- **Next (Beta 1):** a better Windows engine. Options: WinRT/OneCore voices through a small native bridge, cloud TTS,
  or an offline neural TTS (e.g. sherpa-onnx, which would also give offline speech recognition on both platforms).
  Plus speech-to-text for the tutor.

#### ADR-011: Hand-written dependency graph
**Decision.** One `AppGraph` class creates the repositories, scheduler, tutor and ViewModel factories. There's no
DI framework (Koin, Hilt). The graph is small, so a plain constructor is easier to read, needs no annotations or
reflection, and works the same on both platforms. Revisit if it grows past roughly 20 bindings.

#### ADR-012: Versions and build settings
Every version was checked against Maven Central and Google Maven on 2026-10-15, choosing the latest stable:
Kotlin 2.4.21, AGP 9.4.1, Gradle 9.8.1, Compose Multiplatform 1.12.1 (Material 3 1.9.0, icons 1.7.3),
coroutines 1.11.0, serialization 1.11.0, datetime 0.8.0, Ktor 3.6.0, SQLDelight 2.4.1, lifecycle 2.11.0,
activity-compose 1.13.0. Versions are kept in `gradle/libs.versions.toml`.
- AGP 9 specifics: built-in Kotlin (no `kotlin-android` plugin), the `com.android.kotlin.multiplatform.library`
  plugin for KMP modules, and host tests enabled with `withHostTest {}`.
- Android: `compileSdk`/`targetSdk` 37 (Android 17), `minSdk` 26 (Android 8.0, about 97 % of devices, native
  `java.time`). Bytecode is JVM 17; the desktop app is compiled and packaged with JDK 21.
- Gradle: configuration cache, build cache and parallel builds are on. Repositories are centralised in
  `settings.gradle.kts`, with Google Maven limited to Google groups.
- `kotlin.time.Instant`/`Clock` are used instead of kotlinx-datetime's (deprecated) types. kotlinx-datetime is only
  used for time-zone/"today" calculations.

#### ADR-013: How changes are verified
- Unit tests in `commonTest` run twice: on the desktop JVM and as Android host tests. Database tests use an
  in-memory SQLite.
- **Windows GUI:** `gradlew :desktopApp:screenshots` renders every screen off-screen (`ImageComposeScene`) with demo
  data, so the GUI can be reviewed without clicking through it.
- **Android GUI:** the API 36 emulator runs headless under WHPX. The app is installed and screens are captured over adb.
- Screenshots for review are kept in `docs/screenshots/`.

#### ADR-014: Placeholders that must be settled before release
The app name "Lang" (from the folder name), the application id `app.lang` (this one can never change after the
first Play upload), and the launcher and window icons are all temporary. They're tracked in TODO for Beta 2.
