# The universal difficulty axis — grade-mappings seed data

Seed file: `backend/src/main/resources/grade-mappings.csv` (762 rows).
Generator: `docs/gen_grade_mappings.py` — edit the anchor tables and regenerate;
never hand-edit the CSV. The generator asserts uniqueness, per-system
monotonicity, and the cross-system anchor identities before writing anything.

Columns: `gradeSystem, discipline, rawGrade, systemOrder, difficultyScore, confidence, note`.
The `note` column is provenance documentation only — the seeder can ignore it.

## Axis definition

`difficultyScore` is a single shared number line for all systems.
It is anchored on YDS letter rungs at 2.5-point spacing:

```
5.0 = 20.0   5.9 = 42.5   5.10a = 45.0   5.12a = 65.0   5.15d = 102.5
```

Every other system is placed on this line via the consensus of published
conversion charts (Wikipedia's Grade (climbing) tables, Mountain Project's
international chart, ClimbingHouse, Sendy, Bergfreunde, Rockfax/UKC summaries,
AAC/NEice for ice and mixed). Conversions between any two systems are computed
by comparing scores — no pairwise conversion rows exist anywhere.

`systemOrder` is the grade's position within its own ladder (V4 = 4.0,
5.10b = 10.25, Font 6A+ = 6.167). Use it for within-system sorting and
"one grade harder" logic; use `difficultyScore` for anything cross-system.

`confidence` says how much to trust the row's *placement on the shared axis*
(not the grade itself). Comparisons within one system are always exact.

## Placement decisions per system

**YDS (confidence 1.0, variants 0.9–0.95)** — the anchor system.
The ladder starts where Mountain Project's rating picker starts: `3rd`,
`4th`, `Easy 5th` (scores 12.5/15/17.5, systemOrder −3/−2/−1), then `5.0` up.
Slash grades (`5.10a/b`) sit on the letter boundary. Coarse grades follow
Mountain Project's own chart: `5.N-` = 5.Na (synonym row), plain `5.N` =
5.Nb/c, `5.N+` = 5.Nc/d. Sub-5.10 plus/minus (`5.9+`) offset one third of
a rung. Note: `GradeParser` cannot yet route "3rd"/"4th"/"Easy 5th" (its YDS
regex requires `5.x`) — the rows exist, the parser needs a follow-up tweak.

**French sport (0.7 low / 0.85 mid / 1.0 from 7a+)** — from 7a+ = 5.12a
upward the alignment is universal and anchored by benchmark routes that carry
both grades (Action Directe 9a = 5.14d, Biographie 9a+ = 5.15a, Silence
9c = 5.15d), so those rows get 1.0. The 6a–7a band encodes a structurally
forced compression (9 YDS rungs vs 8 French rungs); the seed uses the midline
of the two majority sub-camps (6c = 56.25, i.e. 5.11a/b). Below 6a, charts
disagree by a full rung; placements are midpoints, confidence 0.7.
Slash rows exist in both 8a.nu's short form (`7a/+`) and the full form
(`7a/7a+`), plus letter-boundary forms (`6c+/7a`) — same score, synonym rows.

**V-scale — two ladders: BOULDER 0.5, GYM 0.4** — the boulder→route bridge
is *opinion* (every serious source says so; published charts differ by 2–6
YDS letters at some rungs), and the single largest documented split is gym
vs outdoor: 99Boulders puts outdoor V0 at 5.10d but gym V0 at ~5.8. The seed
therefore carries the spread explicitly as two disciplines:

- `discipline=BOULDER` (outdoor, the *max* end): the published "middle
  line" — V0 = 5.10a/b (46.25), V4 = 5.12a (65.0, the single best-agreed
  rung), V10 = 5.14a, V17 = 5.15d — the plurality consensus, endpoints
  matching the world-top convergence (V17 = Font 9A, 5.15d = 9c).
- `discipline=GYM` (indoor): **a crossover, not a flat shift.** Gyms grade
  soft at the bottom (beginner-friendly) but stiff at the top (comp/board/
  spray-wall setting), so the indoor curve *crosses* the outdoor one. The
  transform `gym_score()` maps each outdoor difficultyScore to its indoor
  counterpart: soft below the crossover (gym V4 = 56.25 vs outdoor 65),
  even around **V10 / 5.14a / score 85** (the crossover), stiff above
  (gym V13 = 96 vs outdoor 92.5), converging at the ceiling (there is one
  hardest climb, indoors or out). Confidence 0.4 — gyms vary wildly, and
  above the crossover this is a modeled offset, not a sourced one.

**One transform, both disciplines.** Because the boulder crossover lands at
difficulty score 85 — which is V10 *and* 5.14a — the same `gym_score()` curve
applies cleanly to routes. So YDS and French now have `GYM` ladders too
(they previously had none): gym 5.10a is soft, gym 5.14a is the crossover,
gym 5.14b+ is stiff. Route gym rows are the modeled offset applied to every
outdoor rung, confidence 0.4.

**Lookup routing rule for consumers**: indoor ticks (the Phase-2 `indoor`
flag; all Kaya gym rows) look up `discipline=GYM`; outdoor ticks use
`BOULDER`/`SPORT`. A tick's own `discipline` stays BOULDER/SPORT either way
— GYM here is a *grading context* in the dictionary key, not a climbing type.
To retune where indoor flips from soft to stiff, edit the `GYM_CURVE` control
points in the generator and regenerate; the assertions re-check that gym is
softer than outdoor low, stiffer high, and equal at the ceiling.

Within both ladders: plus variants sit at the midpoint to the next rung —
reproducing every published Font pin (V3+ = 6A+, V4+ = 6B+, V5+ = 6C+,
V8+ = 7B+) and making `V4+` a synonym of MP's `V4-5` range grade. Minus
variants offset one third of the gap downward (a "soft V4" stays closer to
V4). `VB` and `V-easy` are synonyms.

**Font (BOULDER 0.5, GYM 0.4)** — derived from the V placements (of the
matching discipline) through the near-unanimous V↔Font crosswalk (V0=4,
V1=5, V2=5+, V3=6A, V4=6B, V5=6C, **V6=7A** — the universal anchor —
V7=7A+, V8=7B, V9=7C, then exactly 1:1 to V17=9A). Deriving instead of
placing independently guarantees the V and Font ladders never disagree with
each other, indoors or out. The orphan rung 7B+ sits midway between V8 and
V9, per MP/climbgrades. Note: Font and French look alike (`6A` vs `6a`) —
rows are distinguished by gradeSystem + discipline, never by letter case.
Gym Font rows follow the same crossover as gym V (derived through the
crosswalk), so the indoor boulder ladders stay consistent with each other.

**WI / AI (WI1–6: 0.3, WI7: 0.25, WI8: 0.2, AI: 0.25; Helmcken 0.1)** —
placed via the mixed scale: WI tracks M rung-for-rung up to ~WI6
(Wikipedia/AAC), and M is anchored to rock by the "M+4" rule. Confidence
steps down where the crosswalk exceeds its documented validity (WI7) and
again at WI8, which the AAC lists as effectively undefined. These are rough
movement-difficulty comparisons only. AI sits half a grade below
same-numbered WI — the midline between AAC's "about one grade softer" and
regional no-offset usage. WI10–WI13 (Helmcken Falls spray ice) are included
because they are real logged grades, but flagged 0.1: contested, and WI8/9
were effectively skipped.

**M (0.4 to M7, 0.25 M8–M12, 0.15 above)** — the "M+4" rule anchors
M1–M7 (M4 ≈ 5.8, M7 ≈ 5.11); above M8 the rock equivalence is disputed
(Jeff Lowe said 5.12 for M8; modern charts say 5.11+) and M13+ is
described by sources as highly subjective.

**British E (E1–4: 0.6, E5–9: 0.5, E10–12: 0.4)** — Rockfax/UKC-style
E→French line for *well-protected* routes: E1 ≈ 6a/6a+, E5 ≈ 7a+/7b. The
top end **compresses** onto real-route anchors instead of continuing the
linear slope: E10 = 90.0 (8c+), E11 = 91.25 (8c+/9a — Rhapsody, Lexicon),
E12 = 92.5 (9a — Bon Voyage). Mountain Project's E10→5.15 extrapolation was
rejected as a table-filling artifact. Confidence tiers follow the sources:
agreement within one French letter up to E4, a documented ~2-letter split
between camps at E5–E9, single-route anchoring above. The E grade folds
danger into difficulty (the same physical 7a+ can be E4 safe, E6 runout,
E8 deadly), so no E row exceeds 0.6. Canonical logged forms (`E2 5c`,
including attested off-typical pairs like `E1 5a` and `E10 7c`) are synonym
rows of the bare E rung; the legacy adjectival ladder (Mod … VS, HVS, plus
bare aliases M/D/Sev) sits below E1 at negative systemOrder.

**Aid (no score, no confidence)** — deliberately. No published source places
A/C grades on a free-climbing difficulty axis; the aid axis measures placement
reliability, fall consequence, and time — not gymnastic difficulty. Rows exist
so aid grades are recognized and ordered *within* the system (`systemOrder`),
but they do not participate in cross-system comparison. Any code consuming
this file must tolerate an empty `difficultyScore`.

## What is deliberately not seeded

- Protection suffixes (`5.9+ PG13`, `5.10a R`) — the parser strips the suffix
  and looks up the bare grade.
- Unknown markers (`5.?`, `V?`, `M?`) and snow strings (`Easy Snow`…) — an
  unknown marker *should* fail lookup, and snow has no GradeSystem.
- UIAA, Ewbank, Saxon, Brazilian — not in the app's `GradeSystem` enum yet.
  When one is added, place it the same way: pick the consensus chart, map to
  YDS-anchored scores, regenerate.

## Retuning

To move a system (say, decide V-grades should read two letters softer):
edit that system's anchor dict in the generator and rerun — every variant,
slash, and range row follows automatically, the assertions re-check
monotonicity and the anchor identities, and every existing tick picks up the
new placement because ticks store only a foreign key into this table.
