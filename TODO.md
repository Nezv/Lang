# To-do

## Beta 1 — Functionality, then Reliability
- [ ] Review both GUIs (screenshots in `docs/screenshots/`) and collect feedback
- [ ] AI tutor: try it with a real key in every scenario and tune the prompt; more scenarios
- [ ] Voice: speech-to-text for the tutor; good Chinese TTS on Windows (no voice installed by default)
- [ ] Content: JSON content packs, more HSK 1–3 lessons, audio files, listening/typing exercises
- [ ] Flashcards: card types (listening, reading, production), Anki `.apkg` import, interval fuzz, daily limits
- [ ] Progress: streaks, statistics, daily goal, settings screen
- [ ] GUI polish from review: dark Windows title bar, app icon, Fluent-style option for Windows
- [ ] Reliability: error handling, database migrations, crash reporting, UI tests, CI build

## Beta 2 — Performance, then Security
- [ ] Startup time and memory (desktop JVM image, R8 for Android), bundled CJK font
- [ ] API key storage (Android Keystore / Windows DPAPI) or a backend proxy, privacy review
- [ ] Signed release builds: Play Store bundle, signed Windows MSI
- [ ] Final app name and application id (before the first store upload)
