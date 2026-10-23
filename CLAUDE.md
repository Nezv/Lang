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

When the owner asks for a Chinese or Japanese tutor session, use the `/tutor` skill (`.claude/skills/tutor/SKILL.md`).
The tutor files are in `docs/tutor/`: `Tutor.md` (method, levels, error patterns), `Memory.md` (facts about the
learner), `Vocab.md` (words with maturity) and `conversations/` (one file per session). What the chat sessions teach
us feeds the app's tutor design: `docs/tutor/harness-spec.md`.
