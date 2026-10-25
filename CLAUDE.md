# Lang

Chinese and Japanese conversation tutor, run as chat sessions in the Claude app. The repo holds only the tutor's
files. The Kotlin Multiplatform app that used to live here was dropped on 2026-10-25; its code is in git history.

## Tutor sessions in chat

When the owner asks for a Chinese or Japanese tutor session, use the `/tutor` skill (`.claude/skills/tutor/SKILL.md`).
The tutor files are in `docs/tutor/`: `Tutor.md` (method, levels, error patterns), `Memory.md` (facts about the
learner), `Vocab.md` (words with maturity) and `conversations/` (one file per session).

The repo is public: commit session notes, but don't push unless the owner asks.
