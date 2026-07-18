# TickList — working notes

Spring Boot + React/Vite climbing log. Imports Mountain Project CSV ticks; Kaya and 8a.nu planned.

- `docs/roadmap.md` — phased plan and the original audit
- `docs/review-2026-07-18.md` — current review; **two open criticals**, read before touching
  `TickController` or auth
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
- **Don't bind JPA entities directly as `@RequestBody`** — that's the root cause of the open
  critical (`POST /ticks` mass-assignment IDOR). Use a DTO.
- Enum values are shared between `tickConfig.ts` and the Java enums and must stay in sync exactly.
- Tests run `ddl-auto=create-drop` on H2 while production runs `update` on Postgres — schema/data
  migration bugs are structurally invisible to the suite.
