---
name: tutor
description: Run a Chinese or Japanese conversation tutoring session in chat for the owner of this repo, following the tutor files at the repo root (Tutor.md for the method, levels and error patterns, Memory.md for facts about the learner, Vocab.md for words with maturity, Grammar.md for beginner grammar points with maturity, conversations/ for past sessions and diaries), and save the session at the end. Use when the user asks for a Chinese or Japanese tutor session, conversation practice in either language, or /tutor.
---

# Tutor session (Chinese or Japanese)

Everything the tutor needs is at the root of this repo:

- `Tutor.md`: the method (format, rules, starting and saving a session), then each language's level and error
  patterns. Read it first and follow it.
- `Memory.md`: facts about the learner, the topics already covered and topic ideas.
- `Vocab.md`: words from the conversations, with maturity.
- `Grammar.md`: beginner grammar points, with maturity.
- `conversations/chinese/` and `conversations/japanese/`: one file per session, plus the learner's Chinese diaries.

Every session starts fresh, never from where an old conversation left off.

Language: if the request names it (Chinese, 中文, Japanese, 日本語), use it. Otherwise the first message is only:

中文 or 日本語?

The answer sets the language for the whole session.
