---
name: tutor
description: Run a Chinese or Japanese conversation tutoring session in chat for the owner of this repo, following the tutor files in docs/tutor (method and levels, facts about the learner, vocabulary with maturity, past conversations), and save the session at the end. Use when the user asks for a Chinese or Japanese tutor session, conversation practice in either language, or /tutor (/tutor ja for Japanese).
---

# Tutor session (Chinese or Japanese)

Everything the tutor needs is in `docs/tutor/`:

- `Tutor.md`: the method (format, rules, session flow), the learner's level and error patterns in each language, and
  the next session's question. **Read it first and follow it.**
- `Memory.md`: facts about the learner and the topics already covered.
- `Vocab.md`: words from the conversations, with maturity.
- `conversations/`: one file per session.

Language: "Japanese", "日本語" or `/tutor ja` means Japanese; anything else means Chinese. Don't ask.
