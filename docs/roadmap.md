# TickList — Codebase Audit & Feature Roadmap

## Context

TickList (Spring Boot + React/Vite, evolved from an issue-tracker) imports Mountain Project CSV ticks today. The goals: add Kaya import (a real Kaya export already sits in `inputs/`), then 8a.nu, then build cross-user recommendation features — e.g. use a short climber's ticks/ratings to surface climbs other short climbers will love.

This document is the output of a 70-agent audit: six code reviewers (security, import pipeline, domain/API, frontend, tests/CI, repo hygiene), one adversarial verifier per finding, three feature analysts, and a completeness critic. **55 findings confirmed** against the actual code; 1 refuted (CI-only dummy secrets in `playwright.yml` — acceptable practice).

---

## Part 1 — Issues in the existing code

### Critical — fix before anything else

1. **No per-user authorization anywhere (IDOR)** — `TickController.java:22-75`: `GET /ticks` returns `findAll()` (every user's ticks); GET/PUT/DELETE by id never check ownership; `updateTick` line 51 even copies `user` from the request body, so any caller can reassign or destroy anyone's ticks. The JWT filter already puts the `User` in the SecurityContext — it's just never read. Any self-registered account can read/edit/delete all data.
2. **Imported ticks have no owner** — `Importer.java:91` never calls `tick.setUser(...)`; `ImportController` never resolves the principal. Every imported tick persists with `user = null`.

### High

3. **Re-import duplicates everything** — no idempotency; `externalId` exists on `Tick` but is never set (`Importer.java`). Uploading the same MP export twice doubles the tick list.
4. **No transaction on import** — a bad row mid-file throws and leaves a partial import committed (`Importer.java:34`); `parseOptionalDouble/Integer` throw on malformed numbers.
5. **JPA entities serialized/bound directly** — `GET /ticks` leaks `tick.user` (email, name, and the bcrypt hash field path) and `GradeMapping` (package-private, no getters) will 500 on serialization if ever set (`TickController.java:23`).
6. **Every UI edit silently wipes data** — double bug: backend `updateTick` never copies `pitches/stars/userStars/personalGrade` (`TickController.java:49`), and the frontend PUT payload omits `climbHeight/rawGrade/user` (`tickConfig.ts:149`), so saving an unmodified imported tick nulls those fields (and orphans the tick's owner).
7. **Registration 500s** — missing password → 500 (bean validation never enforced, `UserController.java:61`); duplicate email → unhandled constraint violation 500 → user-enumeration oracle.
8. **Non-array `/ticks` payload crashes the whole page** — no array guard, no error boundary (`TickPage.tsx:49`; documented in `ticklist-issues.spec.ts:86`).
9. **CI never runs backend unit tests** — `playwright.yml` only does `spring-boot:run`; `GradeParserTest/ImporterTest/TickTest` never execute in CI. Also a **stale `test.fail()`** (`ticklist-issues.spec.ts:74`) — that defect is now fixed, so the marker makes the suite fail on "unexpected pass."
10. **Zero auth-boundary/ownership tests at any layer** — which is why issue #1 is live.

### Medium — import pipeline correctness

- `V0-1 PG13` / `V5 R` (protection ratings) parse as `UNKNOWN` (`GradeParser.java`).
- Boulder ticks with Style=`Attempt` classify as `SEND` (`Importer.java:125` only checks lead `Fell/Hung`).
- Kaya JS timestamps truncated to UTC date — evening sessions land on the wrong day (`DateParser.java:47`).
- MP's `-1` "no rating" sentinel stored as a real −1.0 star rating (`Importer.java:80`).
- `skippedRows` is dead code — always 0 (`Importer.java:45`).
- UTF-8 BOM not stripped; no multipart size limits; `ImportBatch` entity exists but is never populated (no import history at all).

### Medium — domain & API

- **Six overlapping grade fields** on `Tick` (`grade`, `rawGrade`, `gradeValue`, `gradeSystem`, `gradeMapping`, `personalGrade`) with no authoritative source; stale client `gradeValue` wins over the grade string.
- No `@Valid` on request bodies (constraint violations → 500 not 400); GET/PUT on missing id returns 200 with empty body instead of 404; no pagination on `GET /ticks`; eager `@ManyToOne` N+1.
- `ddl-auto=update` on Postgres with no Flyway/Liquibase — schema drift and no rollback path.
- **No `Climb` entity** — every tick denormalizes climb data; two users ticking the same climb share no join key. (This is the single biggest blocker for the recommendation goal — see roadmap Phase 3.)

### Medium — frontend

- Auth token lives only in React state (`App.tsx:12`) — any refresh logs you out; no `/me` endpoint exists to rehydrate; expired token = silent broken state.
- Login fetch has no error handling (network failure = dead form); delete failure is an unhandled promise rejection (misplaced try/catch in `TickCard.tsx:170`).
- Search filters on raw enum codes (`CLEAN_TR`) while the UI displays "Clean Tr" — known defect, still present (`TickPage.tsx:99`).
- Imported fields (pitches, avg stars, your stars, personal grade, length, raw grade) are **invisible in the UI** — imported data can't be seen at all.
- `sourceUrl` rendered as raw `href` with no scheme validation (stored-XSS via `javascript:` URL in a crafted CSV); no empty/loading states; one-click permanent delete next to Edit.

### Medium — tests & CI

- Playwright helpers hardcode password `"password"` and honor `PLAYWRIGHT_BASE_URL` with no localhost guard — the suite can write junk accounts/ticks into a deployed environment.
- Test isolation rests on `Date.now()` climb names over a globally shared tick list — strict-mode flakes across parallel projects.
- The only importer test against the real CSVs is gated behind a system property nothing sets; CI serves the frontend from the Vite dev server and never runs `tsc -b` or lint.

### Hygiene

- **`docs/` is gitignored** — the project's only design doc (`ticklist-model.md`) has never been committed.
- No `.env` pattern in any `.gitignore` despite env-var-driven secrets; no root README and `JWT_SECRET` is required-but-undocumented (fresh clone won't boot); `show-sql=true` unconditionally; naming drift (`issuetracker` artifact/DB/class vs `ticklist` package) plus dead duplicated `.github/modernize` dirs and duplicate `/tick`//`/ticks` route aliases.

### Gaps the critic surfaced (verified)

- **CORS hardcodes `http://localhost:5173`** — any real deployment breaks; no dev/prod profiles.
- **`GradeMapping` is a dead subsystem** — no repository, no seed data, no getters/setters (fields unreachable); `gradeValue` is system-local (V-number vs YDS decimal math) — **not cross-comparable**, yet grade pyramids/sorting/recommendations all assume a universal axis. The importer also drops MP's `Rating Code` column — a ready-made numeric difficulty that could seed it. `Grade.java` is dead code.
- **Font vs French distinguished only by letter case** in `GradeParser` — 8a.nu writes both lowercase, so every 8a boulder grade would parse as `FRENCH_SPORT`.
- **No deployment story** (no Dockerfile/compose/profiles/backend CI build) — fine for a laptop, blocking for multi-user.
- **No backend controller/integration tests at all** — auth/IDOR fixes have no harness to land against (need MockMvc + Testcontainers).

---

## Part 2 — Roadmap

### Phase 0 — Security & data-integrity triage (do first; blocks everything)

1. Scope all tick CRUD to the authenticated principal: `findByUser`, ownership checks (404/403), never accept `user` from request bodies; set `tick.user` in the import path.
2. Introduce DTOs (TickResponse/TickRequest) — stop serializing entities; fixes PII leak + GradeMapping 500 + the PUT field-drop bugs on both sides in one stroke.
3. Registration: `@Valid` + 400s, duplicate-email → 409 without oracle behavior; global exception handler (`@RestControllerAdvice`).
4. Frontend session: persist JWT (localStorage), add `GET /me`, rehydrate on load, handle 401 → redirect to login with message.
5. Config: externalize CORS origins per profile; Flyway baseline migration (freeze current schema as V1); remove empty DB-password default; profile off `show-sql`.
6. Test harness to land it: MockMvc/Testcontainers integration tests for ownership on every endpoint; two-account IDOR regression test; remove stale `test.fail()`.

### Phase 1 — Import engine rework (prerequisite for Kaya/8a)

1. Strategy pattern: `SourceCsvParser { source(); supports(headers); parseRow(record) }`; move the MP loop into `MountainProjectCsvParser` verbatim (existing tests stay green); Importer becomes the orchestrator.
2. Per-row try/catch with real counts + row-level errors in `ImportResult` (imported / duplicates / failed with reasons); `@Transactional` import.
3. Header-based source auto-detection → generic `POST /imports`; 400 with detected headers on no match.
4. Idempotency: deterministic `externalId` per row (MP: route URL id + date + style; Kaya: full timestamp + gym + color + grade + ascent_type), unique constraint `(user, sourceApp, externalId)`, skip-and-count duplicates.
5. Finish `ImportBatch`: repository, `Tick → ImportBatch` link, populate per import; enables history and "undo this import."
6. Fix the confirmed parser bugs (protection ratings, Attempt-style boulders, −1 sentinel, BOM, upload size limits).

### Phase 2 — Kaya import

Model additions (one migration): `indoor` boolean (do **not** overload `Discipline.GYM` — Kaya boulders are `BOULDER` + indoor), `holdColor`, `stiffness` (soft/accurate/stiff — Kaya's grade opinion, reused by 8a.nu's soft/hard flags later), optional full `tickTimestamp`.

`KayaCsvParser` mapping (verified against the real export in `inputs/`): `date` → DateParser already handles JS format (keep time in dedup key); `rating` → `userStars` (it's quality stars, **not** the grade — the name collision is exactly why per-source parsers); `ascent_type` Flash/Redpoint/Repeat → SEND + RopeStyle; `grade` (`v4`, `vB`) already parses case-insensitively; **gym rows have empty `climb_name`** (`@NotBlank` would throw) → synthesize "Pink v2 — The Proving Ground"; `gym` (trimmed) vs `location`+country → location + indoor flag.

Tests against fixture rows from the real export (gym row, outdoor row, vB, Repeat, stiffness ±1, trailing-space gym names); import-twice-→-0-new idempotency test; frontend: relabel Upload to auto-detect, show imported/duplicate/failed with row errors.

### Phase 3 — Climb entity & identity resolution (the recommendation keystone)

Do this **with or immediately after** the importers so dedup/backfill is built once, not twice:

1. `Climb` (canonical name, discipline, area FK, canonical grade, consensus stars, height, style tags…), `Area` (hierarchy + aliases), `ClimbExternalRef (sourceApp, externalId) → climb` as the cross-source join.
2. Resolution pipeline at import: exact external-ref match → normalized name+area+discipline match → fuzzy (trigram >0.85, grade ±1 as tiebreaker, store confidence) → else create PROVISIONAL climb. Merge log + reversible admin merge/split (fuzzy matching *will* err both ways).
3. Ticks keep raw imported strings as immutable provenance; backfill migration resolves all existing ticks. **Your own MP+Kaya+8a exports are the first test corpus** — the same boulders must resolve to one Climb.
4. Resurrect the grade axis: `GradeMappingRepository` + accessors + seed data (MP `Rating Code` is a free seed) → one cross-system `difficultyScore`; move consensus facts (stars, height) to Climb; ticks keep personal opinion (`userStars`, `personalGrade`, `stiffness`).

### Phase 4 — 8a.nu import

Task 0: obtain a real export (official path: Profile → Info → Edit → Logbook Export CSV; verify format before writing code). Add `parse(String, Discipline hint)` to GradeParser so Font/French no longer depend on letter case; add slash-grade support (`7A/7A+`). Then `EightACsvParser` is just a third strategy: Onsight/Flash/Redpoint → RopeStyle; toprope flag → CLEAN_TR; soft/hard flags → `stiffness`; comment → notes.

### Phase 5 — Profiles, tags, and the privacy model (design now, before recs)

1. `ClimberProfile`: heightCm, wingspanCm (ape index derived), yearsClimbing, style self-ratings over a **controlled vocabulary shared with climb tags** — so "match short climbers to short-climber-loved climbs" is a SQL join, not NLP.
2. `StyleTag` vocabulary (CRIMPY, SLOPEY, REACHY, DYNO, COMPRESSION, …) + angle + morpho flag (FAVORS_TALL/FAVORS_SHORT/NEUTRAL — the core join for your use case). Stored as `ClimbTagVote(climb, user, tag, confidence, source)` so consensus is computed, never overwritten. Sources: chip-picker at tick time; keyword lexicon over notes ("reachy", "scrunchy", "couldn't reach") at import; LLM extraction later.
3. **Privacy/consent model — resolves a real contradiction**: Phase 0 locks ticks to their owner, but cohort features need to read other users' ticks and height data. Decide now: per-user visibility (private / community-aggregates-only / public), profile opt-in for cohort features, minimum-cohort-size (n≥3) to prevent deanonymization, and add a public `username` handle (cross-user surfaces must never show email/full name — today they would).
4. Per-tick `affinityScore` from implicit signals (repeats are the strongest "loved it" signal; long-projecting-then-send; flash at max grade; notes sentiment/length) — imports are rating-sparse, so this is what makes the corpus usable.

### Phase 6 — Recommendations ladder (each stage useful; don't skip ahead)

- **Stage 0 (n=1, build immediately after Phase 5)**: grade pyramid/gap analysis ("12 V5s, 1 V6 — here are V6 candidates") + content-based recs (tag-vector of loved climbs × tagged unticked climbs in the user's areas, filtered to pyramid band). Works today, remains the cold-start fallback forever.
- **Stage 1 (~20–30 profiled users, pure SQL, explainable)**: cohort filters — "loved by climbers under 5'6″" via cohort-avg-vs-global-delta (surfaces short-person gems, not just universal classics), with minimum-n shown in the UI.
- **Stage 2 (50–100 users)**: rating-correlation neighbors (≥5–10 co-ticked climbs per pair) + tick-overlap Jaccard similarity.
- **Stage 3 (~1k users / 50k ticks)**: implicit-feedback matrix factorization hybridized with content features. Not before — cohort methods beat CF on sparse data and are explainable, which matters for trust.

### Phase 7 — Unique data-driven features (ranked by value/effort)

1. **Per-cohort sandbag index** (high value, med effort): aggregate `personalGrade`/`stiffness` vs consensus deltas by climb/area/cohort → "sandbagged for climbers under 5'6″", "this area runs stiff." Differentiating content no other app has; area-level works with modest data.
2. **Personalized grade prediction** (flagship, needs Stage-2 volume): predicted perceived grade from consensus + morpho flags + height/ape + cohort deltas.
3. **Anti-style trainer**: recommend climbs matching declared weaknesses or tags present in ATTEMPT-but-never-SEND history (failure patterns are signal), at accessible grades.
4. **Morpho inference**: auto-flag climbs where grade-opinion delta correlates with climber height — even if nobody tagged them.
5. **Trip/area planner**: destination + profile + goal grades → ranked hit list by style fit, cohort love, sandbag index (after 1–3).
6. **Partner matching / progression benchmarking** ("climbers your height: median 8 months V4→V6"): defer — needs local user density / longitudinal data.

### Continuous track — product polish & infrastructure (slot into any phase)

- **UI**: sort (date desc default, grade via gradeValue) + discipline/type/date filters; fix enum-label search; expose the invisible imported fields; stats dashboard (sends over time, hardest sends, days out); climb detail pages (post-Phase 3); tick-form ergonomics (autocomplete from own ticks, default date to today, infer gradeSystem, "advanced" section); delete confirmation; auth-aware navbar; empty/loading states; pagination (P1 once imports are real); mobile fixes (navbar wrap, `font-size: 14%` typo in App.css, hardcoded dark background vs light-mode vars); account management (password change, profile); delete dead `RegisterForm.tsx`; lock down `GET /users`.
- **CI/tests**: run `mvnw test` in CI; add `tsc -b` + lint + production build; localhost guard on `PLAYWRIGHT_BASE_URL`; UUID-based test isolation; enable the real-CSV importer test.
- **Repo**: un-ignore `docs/`, commit the model doc; root README (setup incl. `JWT_SECRET`, architecture); `.env` in gitignores; rename `issuetracker` → `ticklist` everywhere; delete `modernize` dirs; drop duplicate route aliases; Dockerfile + compose + dev/prod profiles when deployment matters.

---

## Suggested execution order (summary)

**0. Security triage → 1. Import engine → 2. Kaya → 3. Climb entity + grade axis → 4. 8a.nu → 5. Profiles/tags/privacy → 6. Recs Stage 0 → (data grows) → Stage 1+ cohorts → 7. Sandbag index & beyond.** Phases 0–5 all work at n=1 user and are exactly the things that are cheap now and brutal to retrofit later; the single-user era is when schema migrations and resolver mistakes are painless.

## Verification

- Phase 0: MockMvc/Testcontainers ownership tests (two-account IDOR attempt fails); Playwright suite green after removing stale `test.fail()`.
- Phases 1–2: `mvnw test` with new parser fixtures from `inputs/*.csv`; import the real Kaya export twice → second run reports 100% duplicates, 0 new; MP regression import unchanged.
- Phase 3: import owner's MP + Kaya exports → shared outdoor climbs resolve to single Climb rows; merge/split round-trips.
- Phases 5–6: pyramid/stats page renders from real imported data; content-based recs return tagged, unticked, in-band climbs.
