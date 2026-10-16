# Lang

Chinese learning app for Windows and Android (Kotlin Multiplatform, Compose). Lessons with pinyin, FSRS flashcards,
and an AI conversation tutor. Start with `README.md` (setup, structure), then `CHANGELOG.md` (every decision, as ADRs)
and `TODO.md` (plan).

## Working on the code

- Shared logic lives in `shared/` (content, FSRS, database, tutor engine, ViewModels); each GUI only renders it.
- Checks: `./gradlew :shared:allTests` (and `:desktopApp:compileKotlin`, `:androidApp:assembleDebug` when the
  Android SDK is present). The owner develops on Windows (`gradlew.bat`).
- Record each change in `CHANGELOG.md` under the next version, with an ADR for any design decision.
- The tutor calls the Claude API over raw HTTP (ADR-015). Keys never go in the repo.

## Tutor sessions in chat

When the owner asks for a Chinese tutor session, use the `/tutor` skill (`.claude/skills/tutor/SKILL.md`). The
learner profile is `docs/tutor/learner.md`; session notes are in `docs/tutor/sessions/`. What the chat sessions teach
us feeds the app's tutor design: `docs/tutor/harness-spec.md`.
