# TickList — working notes

Spring Boot + React/Vite climbing log. Imports Mountain Project CSV ticks; Kaya and 8a.nu planned.

- `docs/roadmap.md` — phased plan and the original audit
- `docs/review-2026-07-18.md` — 2026-07 review. C1/C2/H3 resolved (`433c5b2`), M9 + session-expired
  UX shipped (PRs #14/#15 + `f00954f`), H1 fixed 2026-08-06, M4 done 2026-08-08 (Register + Tick
  DTOs). Open: H2, H4, mediums — see the vault Status note or memory for the live plan
  (Flyway baseline → Phase 1 import seam → Kaya)
- `docs/ticklist-model.md`, `docs/grade-axis.md` — domain model and the cross-system grade scale

## Digest cache

This repo has one at `~/.claude/digests/issue-tracker-qa-automation/INDEX.md` (59 files). **Grep it,
don't read it whole.** See `~/.claude/CLAUDE.md` for the full rules and the measured tradeoff.

    grep -i -B2 -A6 "grade\|import" ~/.claude/digests/issue-tracker-qa-automation/INDEX.md

A detached `post-commit` hook keeps it current, so it's usually fine without intervention.

## Build and test

    cd backend && ./mvnw test           # JUnit + Spring integration tests (H2)
    cd frontend && npm run lint
    cd frontend && npm run build        # tsc -b + vite build
    cd frontend && npx playwright test  # E2E; needs backend running

CI gates Playwright behind the backend tests and frontend checks — keep it that way, the ownership
tests are load-bearing.

## Conventions

- Backend is a single flat package, `com.riley.ticklist`.
- Tests: JUnit 5 + MockMvc; `backend/src/test/.../support/ApiTestClient.java` wraps register/login.
- **Don't bind or return JPA entities on controllers** — that caused the (since-fixed) `POST /ticks`
  mass-assignment IDOR. Every endpoint speaks DTOs (`RegisterRequest`, `TickRequest`/`TickResponse`);
  `TickResponse` must stay in step with `TickRecord` in `tickConfig.ts` (Playwright pins this).
- Vite's API proxy target is overridable via `VITE_API_TARGET` (defaults to `http://localhost:8080`).
- Enum values are shared between `tickConfig.ts` and the Java enums and must stay in sync exactly.
- Tests run `ddl-auto=create-drop` on H2 while production runs `update` on Postgres — schema/data
  migration bugs are structurally invisible to the suite.
