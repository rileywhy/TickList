# TODO

Open work only, most important first. **Completed items get deleted, never checked off or struck through** — history lives in git and `docs/roadmap.md`, not here. Detail and rationale for most items: [roadmap.md](roadmap.md).


## Next — before the first real Kaya import

- **V7 migration — land columns BEFORE the first real import** (dedup skips never backfill): `stiffness`, `hold_color`, `indoor`, `tick_timestamp` (timestamptz from day one), plus idempotency — deterministic `externalId` per row (MP: route URL id + date + style; Kaya: full timestamp + gym + color + grade + ascent_type), unique `(user, sourceApp, externalId)`, skip-and-count duplicates. Import-twice → 0 new.
- Wire the new columns in both row parsers once they exist (Kaya stiffness/color/timestamp; indoor from gym column).
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
