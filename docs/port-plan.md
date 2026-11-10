# TickList port plan — Spring Boot + React → Supabase + Expo + PowerSync

Written 2026-11-10. Source of truth for the decision and feature issues is the Linear project
**Tick List** (RIL-118 … RIL-134); this file turns them into work packages anyone (human or
another AI) can pick up without prior context. Each work package (WP) lists its inputs,
deliverables, acceptance criteria and gotchas, and says whether it is safe to hand off.

---

## 0. Context block — paste this at the top of any hand-off session

> TickList is a personal climbing logbook. Today it is a Spring Boot 4 / Java 21 backend
> (single package `com.riley.ticklist`, Flyway migrations V1–V8 on Postgres, custom JWT auth)
> plus a React 19 + Vite web frontend. It imports Mountain Project (MP) and Kaya CSV exports.
>
> **Decision (2026-10-19, RIL-118): port, don't restart**, to:
> - **Supabase** — Postgres, Auth, Row Level Security. No app server. Edge Functions only for
>   small server jobs (never CSV parsing).
> - **Expo (React Native) + Expo Router** — one TypeScript codebase for iOS, Android and web.
> - **PowerSync** — offline-first; SQLite on the device, official Supabase integration.
> - **Cloudflare R2** for photo bytes, **MapLibre** for maps.
> - Dropped: Spring Boot, custom JWT, the Java↔TS enum sync, two deploys.
> - Ruled out: Flutter, separate Swift+Kotlin apps, a plain PWA.
>
> **Order: schema → parsers + tests → config → UI.** Parsers must be proven against the same
> fixtures as the Java code before anything is built on them.
>
> **Hard rules** (do not break these, even if it looks cleaner):
> 1. **Import fingerprints are frozen.** `externalId` = SHA-256 hex of trimmed fields joined by
>    `|`, in a fixed order per source (§WP2). Changing the inputs, order, trimming or date
>    formatting silently breaks dedup for every existing import.
> 2. **No Mountain Project *climb* content ever enters the climb database** (RIL-126). A user's
>    own MP ticks are fine — that's their data. Climb/area data comes from OpenBeta only.
> 3. **Applied migrations are frozen.** Fix forward with a new migration. Migrations are
>    **additive-only** once an app build has shipped (old builds sync against new schemas).
> 4. **No guessed string-length caps.** Use `text`. A length limit is only OK as declared
>    product policy (validated in the UI with a counter).
> 5. **Users can only ever read/write their own rows** (RLS). The old app had a mass-assignment
>    IDOR on `POST /ticks`; the ownership tests that caught it must survive as RLS tests.
> 6. **Provenance fields are not user-settable on the manual path**: a manually created tick has
>    `source_app = 'MANUAL'` and `external_id IS NULL`.
> 7. Enum-like columns are `text`, not Postgres enums or CHECK constraints (decision N1 / V4).
>    The value sets live in one shared TS module.

---

## 1. Where things stand

**Repo today** (`rileywhy/ticklist`, `main` @ `b073406`, last change 2026-10-10):

| Area | Files | Notes |
|---|---|---|
| Schema | `backend/src/main/resources/db/migration/V1–V8__*.sql` | Already Postgres. Tables: `app_users`, `tick`, `import_batch`, `skipped_row`, `grade_mapping`. bigint identity PKs. |
| Grade seed | `backend/src/main/resources/grade-mappings.csv` (816 rows), `docs/gen_grade_mappings.py` | Cross-system difficulty axis. Design: `docs/grade-axis.md`. |
| Parsers | `GradeParser`, `DateParser`, `DisciplineParser`, `MountainProjectRow`, `KayaRow`, `ImportHelpers`, `Importer`, `GradeMappingService` | ~1,000 lines of Java. The valuable part of the backend. |
| Tests (the spec) | `backend/src/test/java/com/riley/ticklist/*Test.java` | `ImporterTest` (644 lines, inline CSV rows), `GradeParserTest`, `DateParserTest`, `GradeMappingServiceTest`, `GradeSeedCoverageIntegrationTest`, `ImportHelpersTest`, `TickOwnershipIntegrationTest`, `TickUpdateProvenanceIntegrationTest`, … |
| Frontend | `frontend/src` (~1,400 lines), `frontend/tests/*.spec.ts` (Playwright) | `tickConfig.ts` holds the enum value lists and tick types. |
| Docs | `docs/ticklist-model.md`, `docs/grade-axis.md`, `docs/roadmap.md`, `docs/TODO.md`, `docs/review-2026-07-18.md` | Keep model/grade-axis as naming source of truth. |
| CI | `.github/workflows/playwright.yml` | backend tests + frontend lint/build gate Playwright. |

Real exports live in `inputs/` and are **gitignored** (personal data). Test fixtures are inline
rows inside `ImporterTest`.

**Linear issues** (all Backlog):

| Pri | Issue | What |
|---|---|---|
| High | RIL-118 | Decision record (this plan) |
| High | RIL-120 | Schema port: client UUIDs, tombstones, additive-only, `auth.users` + profile, RLS, `timestamptz` |
| High | RIL-121 | Imports: parse on device, idempotent per-row upsert (drops the one-transaction import from PR #36 on purpose) |
| High | RIL-122 | Sync rules: never gate local data on the network; silent/manual sync; auth never blocks UI |
| High | RIL-126 | Climb data provenance: OpenBeta only |
| High | RIL-124 | Photo model: EXIF GPS + heading → per-boulder coordinates |
| High | RIL-119 | Spike: free 3D terrain in maplibre-react-native |
| Med | RIL-125 | Photos on R2 with an offline upload queue |
| Med | RIL-123 | Region download (basemap + terrain + climb PMTiles + ticks) |
| Med | RIL-127 | Sensitive-area flag |
| Med | RIL-128 | Indoor grades as gym-scale labels; indoor/outdoor views |
| Med | RIL-129 | To-do / project list |
| Med | RIL-130 | Stats: pyramids, grade breakdown, grade axis in UI |
| Med | RIL-131 | App Store prerequisites |
| Low | RIL-132 | Conditions engine |
| Parked | RIL-133, RIL-134 | Beta video; Apple Watch |

---

## 2. Decisions Riley needs to make first

These block or shape WP1–WP3. Recommendation given for each.

| # | Question | Recommendation |
|---|---|---|
| D1 | New code in this repo or a new repo? | **This repo**, as a workspace: `apps/mobile`, `packages/core`, `supabase/`, `powersync/`. Keep `backend/` + `frontend/` running until cutover (WP9), then delete. "Port, don't restart" — history and docs stay together. |
| D2 | Migrate existing Postgres data or re-import? | **Re-import.** One user, fingerprints make it idempotent, and it doubles as the end-to-end test. No password-hash migration needed. |
| D3 | `grade_mapping` ids | Keep it **server-owned reference data** with stable ids (bigint is fine — it's never created offline). Synced read-only to every device via a global PowerSync bucket. |
| D4 | Import time zone | Currently a global property (`America/Denver`). Move to `profile.timezone`, defaulting to the device zone at signup. |
| D5 | Hosting tiers | Supabase free tier pauses inactive projects; PowerSync Cloud vs. self-host. Check current pricing/limits before committing — not a blocker for local dev (`supabase start`). |
| D6 | Store `raw_row` (jsonb) on imported ticks now? | **Yes, in the schema port.** Parsing on device makes it free, and it lets us promote fields later with a backfill instead of re-asking users for exports. |

---

## 3. Target layout

```
apps/mobile/            Expo app (Expo Router); also builds the web target
packages/core/          Pure TS, zero React Native deps: enums, parsers, fingerprint,
                        grade mapping, import pipeline. Vitest.
supabase/
  migrations/           SQL migrations (frozen once applied)
  seed.sql              grade_mapping seed (generated from grade-mappings.csv)
  tests/                pgTAP RLS / constraint tests
powersync/
  sync-rules.yaml
backend/, frontend/     legacy — deleted at cutover (WP9)
```

---

## 4. Work packages

Legend — **Hand-off:** ✅ good for another AI given this doc + repo access · ⚠️ needs Riley's
review of a judgment call · ❌ needs Riley (devices, accounts, money, decisions).

### WP0 — Golden fixtures from the Java importer  ✅ (do first)

**Why:** "Prove parsers against the same fixtures" needs fixtures with known outputs. The Java
code is the reference implementation; capture its output before it's deleted.

- Add a JUnit test (or small `main`) that runs `MountainProjectRow` / `KayaRow` /
  `GradeParser` / `DateParser` / `GradeMappingService` over fixture CSVs and writes JSON:
  one object per row with every Tick field the parser sets (`climbName, tickDate,
  tickTimestamp, discipline, rawGrade, grade, gradeSystem, gradeValue, difficultyScore,
  tickType, ropeStyle, style, attempts, stars, userStars, pitches, climbHeight, location,
  notes, sourceUrl, personalGrade, stiffness, holdColor, indoor, sourceApp, externalId`)
  or `{skipped: true, recordNumber, reason}`.
- Fixtures: every inline row from `ImporterTest`, plus edge cases (BOM, blank date, malformed
  numbers, midnight-UTC Kaya rows, Font vs French by discipline, protection ratings like
  `5.9 PG13`, `V5 R`, `-1` stars). Committed under `packages/core/fixtures/` (synthetic or
  anonymized rows only — never commit `inputs/`).
- Riley can additionally run it locally over the real `inputs/*.csv` and keep that golden
  output out of git as a private parity check.

**Acceptance:** `packages/core/fixtures/*.csv` + `*.expected.json` committed; regenerating them
is one command.

### WP1 — Schema port to Supabase migrations  ✅ (⚠️ review RLS)

**Inputs:** `V1–V8__*.sql`, RIL-120, `docs/ticklist-model.md`.

**Deliverables:** `supabase/migrations/0001_init.sql` (one clean baseline, not a replay of V1–V8)
plus `supabase/seed.sql`.

- `profile` (`id uuid PK references auth.users on delete cascade`, `first_name`, `last_name`,
  `timezone text`, `created_at`, `updated_at`). Replaces `app_users`. Email/password live in
  `auth.users` (Supabase lowercases emails → N10 is solved).
- `tick`, `import_batch`, `skipped_row`: **`id uuid` primary key, client-generated** (no
  default needed, but `default gen_random_uuid()` is harmless). `user_id uuid not null
  references auth.users`.
- Every synced table: `created_at timestamptz`, `updated_at timestamptz`, `deleted_at
  timestamptz null` (tombstone). `updated_at` maintained by a trigger.
- All timestamps `timestamptz`. `tick_date` stays `date` (a climbing day, not an instant).
- Enum-like columns are `text`; no CHECK constraints (rule 7).
- Carry every column from V1–V8: `attempts, climb_height, climb_id, climb_name, discipline,
  external_id, grade, grade_system, grade_value, location, notes, personal_grade, pitches,
  raw_grade, rope_style, source_app, source_url, stars, style, tick_date, tick_timestamp,
  tick_type, user_stars, grade_mapping_id, import_batch_id, stiffness, hold_color, indoor`;
  `import_batch`: `failed_rows, successful_rows, duplicate_rows (not null default 0),
  imported_at, original_filename, source_app`; `skipped_row`: `record_number, reason, raw_row`.
  Plus `tick.raw_row jsonb` (D6).
- Constraints: `unique (user_id, source_app, external_id)`; check
  `source_app <> 'MANUAL' or external_id is null` (rule 6 — replaces the open TODO about
  manual `POST /ticks` accepting `externalId`).
- Indexes on every FK (`tick.user_id`, `tick.import_batch_id`, `import_batch.user_id`,
  `skipped_row.import_batch_id`) and on `updated_at` for sync.
- `grade_mapping`: same columns as V1 (`grade_system, discipline, raw_grade, system_order,
  difficulty_score, confidence, note, active`), unique `(grade_system, discipline, raw_grade)`.
  `seed.sql` generated from `grade-mappings.csv` (write a small script; don't hand-copy 816 rows).
- **RLS on every table.** v1 policy: own rows only — `using (user_id = auth.uid())` **and**
  `with check (user_id = auth.uid())` for insert/update. `grade_mapping`: select for
  `authenticated`, no writes. `skipped_row` has no `user_id` today — **add one** so RLS doesn't
  need a join.

**Acceptance:**
- `supabase start && supabase db reset` applies cleanly.
- pgTAP tests in `supabase/tests/` port the intent of `TickOwnershipIntegrationTest` and
  `TickUpdateProvenanceIntegrationTest`: user A cannot select/update/delete B's rows, cannot
  insert a row with `user_id = B`, cannot move a row to B, cannot set `external_id` on a
  MANUAL tick, duplicate `(user, source, external_id)` is rejected.

### WP2 — `packages/core`: port the parsers to TypeScript  ✅ (biggest, most valuable hand-off)

**Inputs:** the Java classes in §1, the Java tests, WP0 fixtures, RIL-121.

**Deliverables:** `packages/core` (TS, Vitest) exporting:
- `enums.ts` — `SourceApp, TickType, Discipline, GradeSystem, RopeStyle` as `as const` arrays +
  types. Values **exactly** as the Java enums (including the odd `E_Grade`). This replaces both
  the Java enums and the lists in `frontend/src/tickConfig.ts`.
- `dateParser.ts`, `gradeParser.ts`, `disciplineParser.ts`, `fingerprint.ts`,
  `mountainProjectRow.ts`, `kayaRow.ts`, `gradeMapping.ts` (port of `GradeMappingService`),
  `importCsv.ts` (header detection + row loop, pure: returns ticks / skipped / duplicate
  counts; persistence is the app's job).
- Every JUnit test ported 1:1 to Vitest, plus a parity test that runs all WP0 fixtures and
  deep-equals the expected JSON.

**Port faithfully first, fix bugs second.** Known bugs (`-1` stars stored as −1.0; protection
ratings `5.9 PG13`/`V5 R` parse to UNKNOWN; slash grades `7A/7A+`) get fixed in *separate*
commits after parity, each with a fixture whose expected JSON changes — so behaviour changes are
visible in review.

**Gotchas — Java vs JS differences that will silently break parity:**
- **Fingerprint format** (`ImportHelpers.fingerprint`): each part `null → ""`, then trimmed,
  joined with `|`, SHA-256, **lowercase hex**. Order is frozen:
  - MP: `[routeId(url, route), tickDate as YYYY-MM-DD, rawStyle]`, where `routeId` = digits
    from `/route/(\d+)` in the URL, else the route name.
  - Kaya: `[when, climb_name, gym, color, grade, ascent_type]`, `when` = the instant if Kaya gave
    a real time, else the date.
- **Instant formatting:** Java `Instant.toString()` → `2026-08-24T23:43:39Z`.
  JS `toISOString()` → `2026-08-24T23:43:39.000Z`. **Different string → different
  fingerprint.** Strip `.000` (Java prints fractions only when non-zero).
- **trim():** Java trims only chars ≤ U+0020; JS also trims NBSP, U+FEFF and other Unicode
  spaces. Use a Java-compatible trim inside the fingerprint (and match Java elsewhere).
- **Integers:** `Integer.valueOf("3.0")` throws (row skipped); `parseInt("3.0")` is 3. Use a
  strict `/^[+-]?\d+$/` check. Decimals: `Double.valueOf` — use `Number()` but reject `""`/NaN.
- **Blank dates:** `DateParser` returns empty for a blank date, then the fingerprint line throws
  → the row is **skipped**. Keep that behaviour (explicitly, not via a null crash).
- **Kaya dates:** JS `Date.toString()` format with `GMT+0000 (…)` suffix. **Exactly midnight
  UTC means "date only"** (Kaya's laundered MP/8a rows) → `{date, instant: null}`; never shift it
  into a zone. Otherwise date = the instant converted to the user's zone. Use a real tz library
  (e.g. `date-fns-tz` or Temporal polyfill), not manual offsets.
- **Strict date formats:** `yyyy-MM-dd`, `M/d/yyyy`, `M-d-yyyy`, `MMM d, yyyy`, `MMMM d, yyyy`,
  case-insensitive, STRICT resolving (Feb 30 is rejected, not rolled over).
- **BOM:** strip a leading U+FEFF before header detection.
- **Header detection:** MP = has `Pitches`, `Rating Code`, `Route Type`, `Lead Style`.
  Kaya = has `ascent_type` and `gym`. Otherwise reject the file with the headers in the message.
- **CSV:** RFC 4180 with header row (commons-csv `DEFAULT`). PapaParse with `header: true,
  skipEmptyLines: true` is fine. **Record numbers are 1-based data rows** (`ImporterTest` pins
  the second data row as `recordNumber 2`).
- **Font vs French:** the discipline hint decides (BOULDER → FONT; SPORT/TRAD → FRENCH_SPORT);
  letter case is only the fallback.
- **Dedup within a file and against existing rows**: first occurrence wins; duplicates are
  counted, not saved.
- Regexes: Java `matches()` is whole-string — anchor JS regexes with `^…$`.

**Acceptance:** `npm test` in `packages/core` passes; parity test green over every WP0 fixture;
no dependency on React Native or Node-only APIs (must run in Hermes and the browser — use
`@noble/hashes` or `expo-crypto` behind a small interface for SHA-256, not Node `crypto`).

### WP3 — PowerSync sync rules + client schema  ✅ (⚠️ review)

**Inputs:** WP1, RIL-122.

- `powersync/sync-rules.yaml`: per-user bucket (`tick`, `import_batch`, `skipped_row`,
  `profile` where `user_id = request.user_id()`), a global bucket for `grade_mapping`.
- Client-side PowerSync schema (TS) mirroring WP1 columns.
- Upload connector (`uploadData`) using `supabase-js`: PUT → `upsert`, PATCH → `update`,
  DELETE → set `deleted_at` (tombstone, never a hard delete from the client).
- Treat a unique-violation on `(user_id, source_app, external_id)` as "already there" (no-op),
  not a fatal upload error — this is what makes imports idempotent across partial syncs.

**Acceptance:** two clients signed in as the same user converge after offline edits; a client
of user B never receives A's rows.

### WP4 — Expo app shell + auth  ⚠️

- Expo Router app in `apps/mobile`; iOS, Android and web targets; dev build (not Expo Go —
  PowerSync and MapLibre need native modules).
- Supabase Auth (email/password to start). Session persisted on device.
- **RIL-122 rules:** the app opens straight to the local log with no network and no valid
  session; token refresh happens in the background; sync only on foreground / network-regained;
  no banners over content, no scroll jumps, no sync spinner gating anything.
- Account screen with **in-app account deletion** (RIL-131 — needed for App Store; an Edge
  Function deleting the `auth.users` row, cascading to data).

### WP5 — Tick list + tick form (parity with today's web app)  ✅

**Inputs:** `frontend/src/pages/TickPage.tsx`, `components/TickCard.tsx`,
`components/TickForm.tsx`, `tickConfig.ts`.

- List, create, edit, delete (soft) — all against local SQLite via PowerSync, instant.
- Form ergonomics from RIL-130: two taps to log, obvious undo for an accidental send, date
  picker that doesn't reset.
- Manual ticks always `source_app = 'MANUAL'`, no external_id field in the UI.

### WP6 — On-device import + import summary  ✅

**Inputs:** WP2, RIL-121, `frontend/src/pages/UploadPage.tsx`.

- Pick a file (`expo-document-picker`), parse with `packages/core`, write `import_batch`,
  ticks and skipped rows locally in one SQLite transaction; they sync whenever.
- Per-row idempotency comes from the fingerprint + unique constraint; batch atomicity on the
  server is intentionally gone.
- Summary screen (open TODO item): "Detected: Kaya export", imported / duplicate / failed, list
  of failed rows with reasons. Decide whether to surface unrecognized Kaya `ascent_type` values
  here (today: log-only).
- No file ever goes to a server → the upload-size problem disappears.

### WP7 — Tests & CI  ✅

- CI jobs: `packages/core` Vitest; Supabase `db reset` + pgTAP (`supabase test db`);
  app lint + typecheck; Playwright against the **Expo web build** (port the existing specs —
  auth, register, logout, session, tick — most still apply).
- Keep the rule that E2E is gated behind the unit + RLS jobs.
- Replaces the deferred Testcontainers plan: migrations now run for real in CI.

### WP8 — Map stack spike (RIL-119)  ❌ (needs real devices; can run in parallel with anything)

One day. maplibre-react-native + OpenFreeMap basemap + AWS Terrain Tiles (Terrarium). Pass:
terrain renders at crag zoom on a real iPhone and Android phone with OK frame rate, pitch/rotate
work, a point layer drapes on terrain, works in an Expo dev build. Fallback: Mapbox RN SDK.

### WP9 — Cutover  ❌

1. Re-import Riley's real MP and Kaya exports through the new app (D2). Compare counts and
   spot-check against the private golden output from WP0.
2. Delete `backend/`, `frontend/`, the old CI jobs; rewrite `CLAUDE.md` (build/test commands,
   conventions — most Spring/Flyway/H2 notes go away) and the README.
3. Prune `docs/TODO.md` per §5.

---

## 5. What happens to the current `docs/TODO.md` items

| Item | Fate |
|---|---|
| Manual `POST /ticks` accepts `externalId` | Solved by WP1 check constraint + WP5 form |
| MP parser bugs (`-1` stars, protection ratings) | WP2, after parity, as separate commits |
| Import summary UI | WP6 |
| Re-import `ticks.csv` | WP9 step 1 |
| N2 `/api` prefix | **Obsolete** — no app server |
| timestamptz → `Instant` | Folded into WP1 |
| N10 lowercase email | Solved by Supabase Auth |
| Rate limiting | Mostly obsolete (Supabase Auth rate-limits); revisit for Edge Functions |
| Testcontainers | **Obsolete** — replaced by WP7 |
| Split the flat backend package; rename `issuetracker` | **Obsolete** — backend is deleted |
| `raw_row` JSONB | WP1 (D6) |
| Phase 3 Climb entity, Phases 4–7 | After cutover, on the new stack (§6) |
| Duplicate review | Unchanged; revisit after the first real Kaya import on the new stack |

---

## 6. After the port — feature track (from Linear)

Build in roughly this order; each one is designed to sync like ticks (own rows only in v1).

1. **Phase 3 Climb entity + OpenBeta import** (`climb`, `area`, `climb_external_ref`;
   resolution pipeline; laundered-tick dedup; location overhaul; grade axis surfaced).
   Constrained by **RIL-126**: per-row provenance (`source`, `source_id`, `imported_at`),
   attribution screen (OpenBeta is share-alike), TickList corrections in their own layer.
2. **RIL-129 To-do / project list.**
3. **RIL-130 Stats** — pyramids, grade breakdown, indoor/outdoor filter, all from local SQLite.
4. **RIL-128 Indoor grades** as gym-scale labels mapping optionally onto the difficulty axis.
5. **RIL-124 Photo model → RIL-125 R2 + offline upload queue** (queue built on the sync
   outbox; tick shows its local photo before upload; strip EXIF except deliberate GPS/heading;
   per-photo visibility).
6. **RIL-127 Sensitive-area flag** (needed before any location is shown to other users).
7. **RIL-123 Region download** (PMTiles via tippecanoe on R2; explicit per-area download and
   refresh).
8. **RIL-132 Conditions engine** (needs WP8 terrain + Phase 3 area coordinates).
9. **RIL-131 App Store** — Apple Developer + Play accounts, EAS credentials, Sign in with
   Apple if Google login is added, UGC report/block before any social surface, privacy labels.
10. Parked: RIL-133 video, RIL-134 Apple Watch.

---

## 7. Suggested split

| Who | Work |
|---|---|
| Another AI (ChatGPT/Codex/Claude) | WP0, WP2 (largest), WP1, WP3 draft, WP5, WP6, WP7 |
| Riley | D1–D6 decisions, review RLS (WP1) and sync rules (WP3), WP4 auth feel, WP8 on devices, WP9, accounts and pricing |

Recommended sequence: **D1–D6 → WP0 → WP1 ∥ WP2 → WP3 → WP4 → WP5 ∥ WP6 → WP7 → WP9**, with
WP8 any time. Hand off one WP per session, paste §0 first, and require the acceptance criteria
to pass before merging.
