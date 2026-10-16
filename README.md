# Lang — Chinese learning app (Windows + Android)

Lang combines three ideas:

| Inspiration | What it becomes in Lang | Where |
|---|---|---|
| **Immersive Chinese** | Lessons made of real sentences, with pinyin over each character, tone colours, tap-a-word meanings, listen button, hide/show hanzi, pinyin and English | *Learn / Lessons* |
| **Anki** | Save any sentence as a flashcard; review with Again / Hard / Good / Easy, scheduled by FSRS-6 (Anki's own algorithm) | *Review* |
| **Praktika AI** | Role-play conversations with a tutor, reply hints, pinyin/English toggles, voice | *Tutor* |

**Status:** `0.2.0`, Beta 1. Both apps run with starter content (15 HSK 1 sentences in 3 lessons, 2 tutor role-plays).
The tutor is a real AI conversation partner (Claude) when an API key is set, and an offline script otherwise. See [TODO.md](TODO.md) for what's planned and [CHANGELOG.md](CHANGELOG.md) for every
decision and the reasons behind it.

## Try it

| Platform | How |
|---|---|
| **Windows** | Double-click **`run-desktop.cmd`**. The first start takes a few minutes; later starts take seconds. |
| **Android (emulator)** | Double-click **`run-android.cmd`**. It builds the app, starts the phone emulator and opens the app. |
| **Android (your phone)** | Copy `androidApp/build/outputs/apk/debug/androidApp-debug.apk` to the phone and open it (allow "install unknown apps"). |

The Windows app keeps its data in `%APPDATA%\Lang\lang.db`. On Android it's in the app's private storage.

**Keyboard shortcuts (Windows):** `Ctrl+1…4` switch sections. In **Lessons**: `←`/`→` move between sentences, `Space`
listens, `H`/`P`/`T` toggle hanzi/pinyin/English, `S` saves. In **Review**: `Space` reveals the answer, `1–4` answers, `R` replays.

**Chinese voice on Windows:** this PC has no Chinese voice installed, so the Listen buttons are silent on Windows for
now (the sidebar says so). To add one: *Settings › Time & language › Speech › Add voices › Chinese (Simplified)*.
Android phones normally include one.

## AI tutor setup (private beta)

The tutor talks to the Claude API. Without a key it quietly uses the offline script, and the tutor screen says so
("Offline practice: no API key set"). With a key it says "AI tutor online".

1. Get an API key from the Anthropic Console.
2. Provide it in **one** of these ways (the first found wins):
   - environment variable `ANTHROPIC_API_KEY`, or
   - a file `secrets.properties` in the project folder (it is gitignored) containing `anthropic.api.key=sk-ant-...`.
     On Windows the app also reads `%APPDATA%\Lang\secrets.properties`.
3. Windows: restart the app. Android: the key is read **when the APK is built**, so rebuild with `run-android.cmd` or
   `gradlew :androidApp:assembleDebug`.

Optional: `LANG_TUTOR_MODEL` (or `tutor.model=` in the file) picks another Claude model; the default is `claude-opus-5-5`.
The key is compiled into Android builds made this way, so do not share those APKs. Proper key protection is planned
for Beta 2.

## Screenshots

All of them are in [`docs/screenshots/`](docs/screenshots). Highlights:

| Windows | Android |
|---|---|
| [Today](docs/screenshots/desktop-1-today.png) · [Lessons](docs/screenshots/desktop-2-lessons.png) · [Review](docs/screenshots/desktop-4-review-answer.png) · [Tutor](docs/screenshots/desktop-5-tutor.png) · [Dark](docs/screenshots/desktop-6-today-dark.png) | [Today](docs/screenshots/android-1-today.png) · [Lesson](docs/screenshots/android-3-lesson-player.png) · [Review](docs/screenshots/android-5-review-answer.png) · [Tutor](docs/screenshots/android-6-tutor.png) · [Dark](docs/screenshots/android-7-lessons-dark.png) |

## How the project is organised

```
Lang/
├─ shared/         The "brain", used by both apps: lessons, flashcard scheduler (FSRS), database,
│                  tutor engine, speech interface, and the logic behind every screen (ViewModels).
├─ designsystem/   Shared look: colours, tone colours, Chinese text with pinyin, chat bubbles.
├─ androidApp/     Android GUI: bottom tabs, swipeable lesson pages, big thumb-friendly buttons.
├─ desktopApp/     Windows GUI: sidebar, two-pane lesson browser, Anki-style keyboard review.
├─ docs/           Screenshots of both GUIs.
├─ run-desktop.cmd / run-android.cmd   One-click launchers.
├─ CHANGELOG.md    What changed, with architecture & design decisions (detailed).
└─ TODO.md         Short plan.
```

Both apps share all of their logic, so a feature written once in `shared/` works on both. Each app has its own GUI,
built for its platform.

**Tech stack:** Kotlin 2.4 · Kotlin Multiplatform · Compose Multiplatform 1.12 (Windows) / Jetpack Compose (Android) ·
SQLDelight + SQLite · Ktor · kotlinx-coroutines · Gradle 9.8 · Android Gradle Plugin 9.4 · Android 17 SDK (API 37), minimum Android 8.0.

## Development environment (installed for this project)

Everything is installed **per user, without admin rights or a restart**:

| Tool | Location |
|---|---|
| JDK 21 (Eclipse Temurin 21.0.12.1) | `%USERPROFILE%\.jdks\jdk-21.0.12.1+1` |
| Android SDK (platform 37, build-tools 37/36, platform-tools, emulator, Android CLI) | `%LOCALAPPDATA%\Android\Sdk` |
| Android emulator `medium_phone` (Android 16, Google Play image) | `%USERPROFILE%\.android\avd` |
| Gradle 9.8.1 (downloaded automatically by `gradlew.bat`) | `%USERPROFILE%\.gradle` |

These user environment variables were set: `JAVA_HOME`, `ANDROID_HOME`, and `PATH` additions (`…\jdk…\bin`,
`…\Sdk\platform-tools`, `…\Sdk\emulator`, `…\Sdk\cmdline-tools\latest\bin`). To undo them, delete the folders above
and remove the variables under *Settings › System › About › Advanced system settings › Environment Variables*.

Android Studio or IntelliJ IDEA can open this folder directly if installed later. Both detect the JDK and SDK above.

If Windows Firewall asks about `adb.exe` (the Android emulator bridge), you can click **Cancel**: it only needs local access.

### Developer commands

```bash
./gradlew.bat :desktopApp:run                 # run the Windows app
./gradlew.bat :androidApp:assembleDebug       # build the Android APK
./gradlew.bat :shared:allTests                # run the shared tests (on the desktop JVM and the Android host)
./gradlew.bat :desktopApp:screenshots         # render every Windows screen to desktopApp/build/screenshots
./gradlew.bat :desktopApp:createDistributable # build a self-contained Lang.exe (desktopApp/build/compose/binaries/main/app/Lang)
```
