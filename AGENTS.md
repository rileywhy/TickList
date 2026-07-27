# AGENTS.md — TickList

Instructions for any AI coding agent working in this repo (Codex, Claude Code, Cursor, etc.).

## What this project is

TickList: a climbing logbook. Spring Boot (Java 17+, Maven) backend in `backend/`, React + Vite +
TypeScript frontend in `frontend/`, Playwright E2E tests in `frontend/tests/`. It imports Mountain
Project CSV exports; Kaya and 8a.nu are planned.

- `docs/roadmap.md` — phased plan and the original audit
- `docs/review-2026-07-18.md` — current code review (two open criticals; read before touching
  `TickController` or auth)
- `docs/ticklist-model.md`, `docs/grade-axis.md` — domain model and the cross-system grade scale

## Orient with the digest cache

There's a file-digest cache **outside this repo** (deliberately — it's generated data and would
otherwise churn in every PR):

    ~/.claude/digests/issue-tracker-qa-automation/INDEX.md      # readable
    ~/.claude/digests/issue-tracker-qa-automation/digests.json  # machine-readable

On Windows that resolves to `C:\Users\firef\.claude\digests\issue-tracker-qa-automation\`.

It holds, for every source file, a 2-3 sentence summary of what the file does and anything
non-obvious about it, plus its exact public symbols and imports.

**Grep it — don't read it whole.** Reading all of `INDEX.md` costs ~10k tokens and is usually a net
loss. A targeted grep costs ~1k:

```bash
D=~/.claude/digests/issue-tracker-qa-automation/INDEX.md
grep -i -B2 -A6 "auth\|jwt\|login" "$D"    # by concept
grep -B2 -A6 "applyGradeMapping" "$D"      # by symbol
```

**Workflow:**

1. Grep the index for your task's keywords or symbol names.
2. Identify the 2-3 files that actually matter.
3. Open only those in full.
4. Widen only if what you find contradicts the digest, or the task genuinely spans more.

Measured on an identical impact-analysis task (2026-07-18), same model, three runs:

| approach | tokens | vs no cache |
|---|---|---|
| grep the index | 52,122 | **−8%** |
| no cache at all | 56,615 | — |
| read the index end-to-end | 59,351 | **+5%** (worse than no cache) |

The saving is modest and **inverts if you read the index whole**. If you already know which file you
need, skip the index entirely — it's overhead. Break-even is roughly 4 files.

**`digests.json`** is keyed by repo-relative path — grep `purpose` to locate a concern, grep
`symbols` to find a definition without a full-text search:

```json
{
  "backend/src/main/java/com/riley/ticklist/DateParser.java": {
    "blob": "<git blob hash>",
    "purpose": "This file parses various date string formats into LocalDate objects...",
    "symbols": ["class DateParser", "parse()"],
    "imports": [],
    "lines": 61
  }
}
```

### Rules

- **The code is the truth.** Digests are generated summaries. If one contradicts the source, the
  source wins — open the file. Never make a change based on a digest alone.
- **Symbols and imports are exact** (regex-extracted, not generated), so they're safe for
  navigation. Only the `purpose` prose is model-written.
- **Fail open.** The cache is an optimization, never a dependency. If it's missing, stale, or wrong,
  just read the source and continue. Never block work on it.
- **Don't hand-edit** the cache files — they're regenerated and your edits will vanish.

### Refreshing

Incremental, keyed on the git blob hash, so it only regenerates files whose content changed:

```bash
python ~/.claude/tools/build_digests.py --check    # staleness only (instant, no model needed)
python ~/.claude/tools/build_digests.py            # incremental refresh
```

A detached `post-commit` hook keeps it current automatically. Generation needs a local ollama server
on `localhost:11434`; **without it the script exits cleanly and changes nothing.** Reading the
existing cache never requires ollama. Files that fail to summarize retry on the next run; failures
log to `~/.claude/digests/issue-tracker-qa-automation/refresh.log`.

## Build and test

```bash
cd backend && ./mvnw test          # JUnit + Spring integration tests (H2)
cd frontend && npm run lint        # ESLint
cd frontend && npm run build       # tsc -b + vite build
cd frontend && npx playwright test # E2E; needs backend running
```

CI (`.github/workflows/playwright.yml`) gates Playwright behind the backend tests and frontend
checks — keep it that way, the ownership tests are load-bearing.

## Conventions

- Backend is a single flat package, `com.riley.ticklist`.
- Tests: JUnit 5 + MockMvc; `backend/src/test/.../support/ApiTestClient.java` wraps register/login.
- **Don't bind JPA entities directly as `@RequestBody`** in new endpoints — that's the root cause of
  the open critical (`POST /ticks` mass-assignment IDOR). Use a DTO.
- Enum values are shared between `tickConfig.ts` and the Java enums and must stay in sync exactly.
- Tests run `ddl-auto=create-drop` on H2 while production runs `update` on Postgres, so schema and
  data-migration bugs are structurally invisible to the suite.
