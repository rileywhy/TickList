# TODO

Open work only, most important first. **Completed items get deleted, never checked off or struck through** — history lives in git and `docs/roadmap.md`, not here. Detail and rationale for most items: [roadmap.md](roadmap.md).


## Now — two shipped unique constraints (V6, V7) can already 500 in prod

*(Confirmed 2026-09-13 against current `main`; first flagged 2026-09-12 on an unmerged review
branch that never reached this file — see note at the bottom of this section.)*

- **No handler for `DataIntegrityViolationException` anywhere** (`ApiExceptionHandler` only catches `MethodArgumentNotValidException`). Two constraints that already shipped are reachable without any further code change:
  - `idx_app_users_lower_email` (V6) shipped without N10's paired app-side half (`trim().toLowerCase()` at register/login — still open below). `UserController.register` only rejects an *exact*-case duplicate (`userRepository.findByEmail`); a case-variant duplicate (`Foo@x.com` then `foo@x.com`) sails past that check and hits the DB constraint instead → unhandled 500 where the existing code already has a clean 409 for the exact-case case.
  - `uk_tick_user_source_external` (V7) shipped while `TickController.applyRequest` still copies client-supplied `sourceApp`/`externalId` unchanged on both POST and PUT with no conflict check — a PUT that sets them to match another of the user's ticks 500s instead of failing cleanly.
  - Neither is caught by the test suite (H2 `create-drop`, Flyway disabled — doesn't see Postgres constraint behavior at all). Fix: add a generic `@ExceptionHandler(DataIntegrityViolationException.class)` → 409, on top of whichever of N10/N11's app-side fixes lands first.
  - Doing that alone isn't enough for CSV import: `Importer.importCSV`'s per-row loop only try/catches the *parsing* step (`processKayaRow`/`processMTNProjectRow`) — `tickRepository.save(tick)` sits outside that try/catch, so once `externalId` is actually wired in (below) a single duplicate row throws out of the whole method and 409s the entire batch instead of the "skip-and-count duplicates" behavior the row below promises. The save call needs its own catch alongside the parse-error one.
- **Kaya rows have no path to `TickType.ATTEMPT`**: `KayaRow.processKayaRow` never calls `ImportHelpers.classifyTickType` (only `MountainProjectRow` does) — it derives `TickType` purely from whether `ascent_type` resolves to a `RopeStyle`; unresolved and "genuinely a failed attempt" both land in `TickType.UNKNOWN`, indistinguishably. Check Kaya's real ascent_type vocabulary (not just the fixture values) for attempt/fall wording before the first real import.
- **Process note**: this section's findings were independently reproduced by three separate scheduled review runs (2026-08-20, 2026-09-10, 2026-09-12) but each committed only to its own disposable `claude/exciting-ramanujan-*` session branch, never merged — so this file, on `main`, hasn't reflected them until now. Worth deciding whether the scheduled review should target `main` directly, or someone periodically sweeps the stranded branches.

## Next — before the first real Kaya import

- **V7 migration — land columns BEFORE the first real import** (dedup skips never backfill): `stiffness`, `hold_color`, `indoor`, `tick_timestamp` (timestamptz from day one), plus idempotency — deterministic `externalId` per row (MP: route URL id + date + style; Kaya: full timestamp + gym + color + grade + ascent_type), unique `(user, sourceApp, externalId)`, skip-and-count duplicates. Import-twice → 0 new.
- Wire the new columns in both row parsers once they exist (Kaya stiffness/color/timestamp; indoor from gym column) — right now `stiffness` and `color` are read off the CSV row and then dropped (`color` only reused as a climb-name fallback; `tick.setStiffness(...)` is commented out), so `hold_color`/`stiffness`/`indoor`/`tick_timestamp` stay null on every Kaya tick imported today.
- DateParser: full-timestamp variant — JS-format dates currently truncate to UTC date, evening sessions land on the wrong day.
- Import robustness: `@Transactional` import, raise 1 MB multipart cap (N4/N12), strip UTF-8 BOM.
- Remaining MP parser bugs: `-1` "no rating" sentinel stored as real −1.0 stars; protection ratings (`5.9 PG13`, `V5 R`) parse to UNKNOWN grade.
- Import summary UI: show "Detected: Kaya export" + imported/duplicate/failed with row errors. Open question: surface unrecognized ascent_type values to the user via ImportResult (currently log-only).
- Re-import `ticks.csv` through the UI (795 ownerless pre-auth ticks were deleted; this also end-to-end-verifies V3–V6).

## Soon — deployment-horizon fixes (N-items, detail in review-2026-07-18.md addendum)

- N2: `/api` URL prefix — SPA routes vs API routes collide; also a prerequisite for clean shareable paths (universal links, below).
- timestamptz → `Instant` ripple through Java (own session).
- N10 app half: lowercase email on register/login.
- Rate limiting, remaining N-items as they bite.

## Phase 3 — Climb entity & identity resolution (the keystone)

- `Climb`, `Area` (hierarchy + aliases + lat/long/aspect/rock type — coords exist in the OpenBeta dump), `ClimbExternalRef` as the cross-source join.
- Resolution pipeline at import: external-ref → normalized name+area → fuzzy with confidence → PROVISIONAL; reversible merge/split with log.
- Cross-source laundered-tick dedup (Kaya ingests MP/8a/Sendage; the externalId constraint can't catch those — same user/climb/date heuristic lives here).
- Location overhaul: `Tick.location` carries three dialects (MP breadcrumbs, Kaya join, free text); per-source splitters, raw string stays as provenance, `indoor` set at import time.
- Resurrect the grade axis: GradeMappingRepository + seed (MP `Rating Code` is a free seed) → cross-system `difficultyScore`.
- Test corpus: owner's MP + Kaya exports must resolve shared boulders to one Climb.

## Phase 4 — more importers

- 8a.nu: get a real export first (Profile → Info → Edit → Logbook Export), then a third row class; GradeParser slash-grade support (`7A/7A+`).
- Kaya's logbook template (`inputs/logbook_template.csv`): 3-line preamble to skip, `Climb Type` column resolves Font-vs-French; imports anyone who formatted a spreadsheet for Kaya.
- Sendage (needs a real export); eventually publish our own bring-your-own-spreadsheet template.

## Phase 5 — profiles, tags, privacy (design before recs)

- ClimberProfile (height/wingspan/style self-ratings), StyleTag vocabulary + morpho flags, ClimbTagVote.
- Privacy/consent model: per-user visibility, cohort opt-in, min-cohort-size, public `username` (cross-user surfaces must never show email).
- Per-tick `affinityScore` from implicit signals.

## Phase 6/7 — recommendations & data features (gated on data volume)

- Stage 0 recs at n=1: pyramid/gap analysis + content-based recs. Then cohort filters → neighbors → matrix factorization as users grow.
- Sandbag index, personalized grade prediction, anti-style trainer, morpho inference, trip planner.
- Conditions engine & day/session planner (works at n=1 once Phase 3 Areas have coords — see roadmap 7.7).

## Continuous track (slot in anywhere)

- Split the flat backend package after Kaya merges: by feature, own commit, update CLAUDE.md.
- UI polish list (roadmap continuous track): sort/filters, stats dashboard, tick-form ergonomics, pagination, mobile fixes, account management.
- Tests/CI: Testcontainers to make migration bugs visible to the suite; localhost guard on `PLAYWRIGHT_BASE_URL`; UUID test isolation; enable the real-CSV importer test.
- Repo: root README, `.env` in gitignores, rename `issuetracker` → `ticklist`, Dockerfile + compose when deployment matters.
- iOS/Android (future): keep API client-agnostic; universal links (shared links open the app — needs clean URL paths); React Native over Swift if Android stays wanted.
- Deferred consciously: H2/H4 (pre-leaderboard), tick.spec.ts parallel flake.
