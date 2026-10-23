# Tutor session retrospective: 2026-10-16

The first chat-based tutor session ran in Claude Code (cloud). Claude Sonnet 5.5 was the tutor, with about 22 learner
messages in roughly an hour. The full notes are in [conversations/2026-10-16-chinese.md](conversations/2026-10-16-chinese.md). This review covers
what to keep, what went wrong, and what the app's tutor harness must do better
([harness-spec.md](harness-spec.md)).

## Scores

| Area | /10 | Verdict |
|---|---|---|
| Format adaptation | 8 | The learner's table format took 4 rounds of feedback to reach. After that the tutor kept it every turn. |
| Reading garbled input | 7 | Most guesses were right, marked "my best guess", and the tutor asked when unsure. One guess reversed the meaning (少贵 was read as "not too expensive"). |
| Correction accuracy | 7 | Two false alarms: 每天吗 (a valid echo question) and 是同一回事吗 (common, not stiff) were marked ⛔. |
| Level calibration | 3 | About 50 new words and 8 grammar patterns in one hour for a beginner. Grammar explanations were written in Chinese above the learner's level. |
| Pacing | 4 | A new connective came almost every turn and none were reviewed. The learner had to stop it twice ("we stopped the connective thing", "no new words"). |
| Vocabulary grounding | 2 | The tutor had no access to the learner's Immersive Chinese or Anki words and didn't ask for them. |
| Engagement | 8 | Topics came from the learner's life (office tea, instant coffee in milk, MLBB, 200 g burgers, Japan), and each question built on the last answer. |
| **Overall** | **6** | A good conversation partner with a poor curriculum fit. |

Most failures were missing context (no profile, no vocabulary, no rules at the start) and pacing judgement, not model
capability. A harness fixes the first. Explicit rules plus a validator fix the second.

## What worked (keep)

- **One question per turn, plus a 💡 scaffold.** The learner always had a sentence frame to fill.
- **✴️ / ⛔ / ✅ rows, one per sentence fragment.** They show what was fine and what wasn't, without a lecture.
- **Hanzi, pinyin and English in one cell, stacked vertically.** The learner asked for this layout and read every row.
- **Diagnosing the pinyin-keyboard pattern.** 查/茶, 并/冰, 旺季/忘记, 白/百, 日月/日语 (and probably 香曲/想去) are
  wrong characters picked from the IME candidate list, not grammar errors. This was the most useful single finding of
  the session, and it needs its own category (see the spec).
- **Admitting misreads** (milk price, echo question), which keeps the learner's trust.
- **Literal English glosses of the ⛔ fragments** ("check sell", "merged coffee"). They show how a native reader sees the slip.

## What went wrong

1. **No context at the start.** The repo has no `CLAUDE.md`, no learner profile and no vocabulary list. The tutor said
   "I don't know what lesson 80 teaches" and moved on. It should have asked once for the word list or Anki export,
   then limited itself to those words.
2. **Too many new words.** Here is the tutor's own vocabulary, beyond the learner's: 连词, 口味, 有点, 毫升, 一大勺, 速溶, 糖, 夏天,
   双排, 排位, 英雄, 手机游戏, 免费, 好玩, 面包, 牛肉, 芝士, 蔬菜, 切达, 培根, 酱油, 洋葱, 克, 之类的, 声调, 语法, 进步, 担心,
   规律, 输入法, 强调, 别人, 差不多, 怀疑, 惊讶, 主语, 话题, 以前, 女孩, 日语… A beginner can absorb 5 to 10 a session.
3. **A connective treadmill.** 和/但是 → 因为…所以 → 如果…就 → 还是 → 虽然…但是 → 一边…一边 → 先…然后 → 不但…而且
   came in about ten turns. The learner used each one once correctly, and none came back later.
4. **Explanations in Chinese.** Examples are "拼音输入法选错了字" and "说一串东西时,用、隔开". The explanation was harder
   than the mistake. Explanations should be one line of English.
5. **English words in the learner's sentences were treated as lessons.** For milk, flavours, grams, soluble, cheddar,
   bacon and anime, each became a new Chinese word on the spot. These are *gaps* and should be recorded, not taught mid-sentence.
6. **The format wasn't saved.** The learner described the layout across four messages, and nothing would carry it into the next session.
7. **False positives are expensive.** A beginner trusts ⛔ completely. Mark ⛔ only for clear errors. Mark "fine, but
   here's another way" differently.
8. **Some turns asked several questions** before the learner set the one-question rule.

## Learner level (estimate from ~22 messages)

**Production is around HSK 1 with some HSK 2 structures. Comprehension is higher with pinyin and English support.** The
learner wants to say adult things (instant coffee recipe, duo queue, motivation) beyond their vocabulary. That's good for
engagement, but it produces garbled sentences.

| Can do | Struggles with |
|---|---|
| S + 喜欢/想/觉得/是/有 + O | Word order of time, place and companion phrases: 在…的时候, 和…一起, 带…到 |
| 因为…所以, 如果…就, 虽然…但是, 一边…一边 after one example | Quantity after the verb: 喝**很多**咖啡 |
| 一般, 每天都, 的时候 | 很 before adjectives, 的 after 自己 / before nouns |
| Questions with 吗 / 什么, echo questions | 二 vs 两, 和 in lists, 还是 vs 和 |
| Asking meta questions in Chinese | Verb choice: 好吃/好喝, 记得/觉得, 做 used as a filler verb |
| Numbers | 了 for "wanted" (想了) |
| | **IME homophones (6 times this session)** |

## Words

**Gap list** (said in English or garbled, worth flashcards *if they aren't in the deck yet*): milk 牛奶, flavour 口味,
gram 克, bring 带, instant 速溶, iced 冰, forget 忘记, Japanese (language) 日语, girl 女孩, before 以前, anime 动漫.

The other tutor-introduced words above were seen once. They shouldn't count as learned.

## Why the Immersive Chinese vocabulary was missing

Nothing was imported, so there was nothing to find:

- The repo holds only the 15 seeded HSK 1 sentences (`shared/.../content/SeedContent.kt`).
- Anki `.apkg` import is still an open TODO item.
- There is no `CLAUDE.md` pointing a session at a learner profile or word list.
- A cloud session can't reach Anki on the learner's PC. The deck has to be exported into the repo (or, later, into the app).
- The tutor read only the top of the README, and it didn't ask for the deck.

**Action for the learner:** in Anki, use *File › Export › Notes in Plain Text* (keep the fields), for the Immersive
Chinese deck, and save it as `content/vocab/known.tsv`. Commit it only while the repository is private: course content
belongs to its publisher.

## Takeaways → actions

| Lesson | Process fix (Claude Code, done now) | App fix (harness spec) |
|---|---|---|
| No context at start | `CLAUDE.md` + `/tutor` skill + learner profile (now `Tutor.md`, `Memory.md`, `Vocab.md`) | Learner profile and known-vocabulary store in the DB |
| Too many new words | Skill rule: new-word budget, default 0 | Budget field + deterministic vocabulary validator |
| Connective treadmill | Skill rule: at most one new pattern per session, then recycle | Session goal = one target pattern + due words |
| Chinese explanations | Skill rule: explanations in one English line | `note` field is English, length-capped |
| English words as lessons | Skill rule: record gaps, don't teach | `gaps[]` in the turn schema → "add to deck" |
| Format not saved | Format in the skill | Correction card UI = this format |
| False positives | Skill rule: ⛔ only when sure, 🔷 for alternatives | Segment kind `alternative` |
| IME slips | Skill rule: tag as typing slip | Segment tag `ime_homophone`, own stats |
| No regression check | Session notes kept as eval material | Eval set built from this transcript |
