# Tutor harness: specification (draft)

Goal: the app's Tutor gives the conversation from the 2026-10-16 chat session, built from the learner's own
vocabulary, with natural conversations on personal topics instead of drills. The reasons behind each requirement
are in [retrospective-2026-10-16.md](retrospective-2026-10-16.md).

The current tutor (0.2.0, ADR-015) is scenario role-play at a fixed "HSK 1–2" level. It returns one English
correction sentence and two suggestions. This spec extends it, and keeps the raw-HTTP engine, structured output and
scripted fallback.

## 1. Learner model (new, persisted in SQLite)

| Part | Contents | Source |
|---|---|---|
| **Profile** | Level label ("Immersive Chinese 80"), new-word budget per session (default 0), max new patterns per session (default 1), explanation language (English), display toggles | Settings screen |
| **Known vocabulary** | hanzi, pinyin, meaning, origin (Anki deck / lesson / tutor), FSRS state (new, learning, review), due date | Anki import (`.apkg` or plain-text export), lessons, accepted tutor words |
| **Facts** | Short things the learner told the tutor ("drinks tea at the office", "plays MLBB daily with girlfriend", "wanted to study in Japan") | `facts[]` from turns, learner can delete |
| **Error stats** | Count per tag (word order, measure word, 很, 了, IME homophone, …) and the last examples | `segments[].tag` from turns |
| **Sessions** | Date, mode, summary, open question, new words, gaps | Session end |

## 2. Prompt assembly (cache-friendly)

Order matters because prompt caching matches the prefix byte for byte:

1. **System, frozen:** tutor rules + output rules. It changes only with app releases.
2. **System, stable per learner:** profile, known-vocabulary list (hanzi + pinyin only, one per line, sorted), facts,
   top error tags. It changes when the deck changes, so cache it with a breakpoint after it.
3. **Messages:** a session-goal cue first (mode, target pattern if any, up to 5 due words to recycle, last session's
   open question), then the conversation.

Notes:
- About 1,000 known words is a few thousand tokens. That's fine to send every turn when cached. Check
  `usage.cache_read_input_tokens` is non-zero from the second turn on.
- Keep `effort: "low"` for chat turns (current setting). The validator (§4) catches what low effort misses.
- Model: keep the current default (`claude-opus-5-5`). Any change is the owner's call.

## 3. Turn schema (structured output)

Every hanzi/pinyin/English triple is a `Line {hanzi, pinyin, english}`.

```jsonc
{
  "segments": [                      // the learner's last message, split into fragments (empty on the first turn)
    {
      "kind": "natural | error | alternative | english",   // ✴️ / ⛔ / 🔷 / English island
      "said": Line,                  // what the learner wrote, with a literal English gloss
      "fix": Line | null,            // for error / alternative / english
      "tag": "word_order | measure_word | hen | le | particle | verb_choice | ime_homophone | missing_word | other"
    }
  ],
  "corrected": Line | null,          // the whole message, natural (null if already natural)
  "clarify": {                       // only when the meaning can't be recovered; replaces the question
    "guess": Line,                   // "Did you mean …?"
  } | null,
  "reply": [Line],                   // 1–2 short lines reacting to the content
  "question": Line | null,           // exactly one question (null only when clarify is set or the session ends)
  "hint": Line,                      // 💡 sentence frame for the answer
  "note": "string",                  // optional, one line of English, ≤ 120 characters
  "new_words": [Line],               // words used in reply/question that are not in the known list; ≤ budget
  "gaps": [Line],                    // words the learner wrote in English or couldn't say, with the Chinese
  "facts": ["string"],               // new personal facts worth remembering
  "finished": false
}
```

Rules in the system prompt (from the retrospective):
- Use only known words, plus up to the session's new-word budget, declared in `new_words`.
- Ask questions the known words can answer. A specific question (which dinosaur? which item?) forces words the
  learner isn't learning; prefer time, place, people, feelings and reasons. Words that only the tutor's question
  forced don't go into the vocabulary list.
- Introduce at most one new grammar pattern per session, then reuse it.
- Ask exactly one question per turn.
- Write explanations in one line of English, never in Chinese.
- Mark `error` only for clear mistakes. Use `alternative` when the learner's sentence is acceptable.
- Words the learner wrote in English go into `gaps`. Don't teach them mid-sentence.
- When the learner's word is a homophone of the intended one (same or similar pinyin), use the tag `ime_homophone`.
- If unsure what the learner meant, use `clarify` rather than guessing silently.
- Never ask for something the learner has already told the tutor, this session or in `facts`. Build on it instead.

## 4. Validator (deterministic, in `shared`)

It runs on every turn before display. On failure, retry the turn once with the failure appended as a system message.
If the retry also fails, show the turn with a warning flag.

| Check | How |
|---|---|
| Vocabulary within budget | Segment the Chinese text of `reply` and `question` by forward maximum matching against known words + `new_words` + a dictionary (CC-CEDICT, CC BY-SA; check attribution). Unknown words not declared in `new_words`, or too many declared, fail the check. |
| One question | `question` is set, or `clarify` is set, but not both; `reply` contains no `？`. |
| Pinyin sanity | The syllable count equals the Han character count in each `Line`, and tone marks are present (reuse `Pinyin`). |
| Explanation language | `note` contains no Han characters, ≤ 120 characters. |
| Segments cover the message | Concatenated `said.hanzi` ≈ the learner's message (ignoring punctuation and spaces). |

## 5. UI (both apps)

- **Correction card** under the learner's bubble: one row per segment, with the emoji column (✴️ ⛔ 🔷 ✅), each cell
  stacked hanzi / pinyin / English, and the whole-sentence ✅ last. Collapsed to the ✅ row on small screens. Tap
  ✅ → **Save as card** (sentence card, FSRS).
- **Tutor bubble:** reply lines + the single question, highlighted. The hanzi/pinyin/English toggles already exist.
- **💡 hint:** tapping it inserts the frame into the input box for the learner to complete. It doesn't send it.
- **Clarify turn:** "Did you mean …?" with **Yes** / **No, let me retype** buttons.
- **Typing slips:** an `ime_homophone` segment shows a ⌨ badge ("typing slip: 查 → 茶") so it isn't mistaken for a
  grammar error. Its own count appears in statistics.
- **Words panel:** `gaps` and `new_words` collect in a side panel (Windows) or bottom sheet (Android) with
  **Add to deck** per word. Nothing enters the deck without a tap.
- **Session end:** summary screen (corrections, patterns, words, open question) + "Add all to deck". Saved to
  history. The **Today** screen offers "Continue: 你现在还想去日本吗?".
- **Modes:** *Free talk* (personal topics from facts), *Scenario* (current role-plays), *Pattern practice* (one target
  pattern, e.g. from the current lesson, using due words).

## 6. Evals

Build an eval set from saved sessions before tuning the prompt. Start with the 22 learner messages of 2026-10-16.
Each entry has the message, the conversation so far and the expected intent of the correction. Grade with the
validator (§4) for the hard rules, and with a rubric (meaning preserved, no false ⛔, natural correction) for the rest.
Run it on every prompt or model change.

## 7. Phases

| Phase | Scope |
|---|---|
| **A** | Anki import (plain-text export first, then `.apkg`), known-vocabulary table, profile settings |
| **B** | New turn schema + system prompt + validator, behind the existing `TutorEngine` interface; scripted fallback unchanged |
| **C** | Correction card, hint insert, clarify buttons, words panel, save-as-card |
| **D** | Facts, error stats, session summary, Today "continue", free-talk and pattern modes |
| **E** | Eval set from saved sessions; prompt tuning against it |
