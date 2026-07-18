"""Generate grade-mappings.csv, the project difficulty-axis seed data.

Two numeric axes appear in the output:
- systemOrder: position within one grading system, for sorting and rung math.
- difficultyScore: shared project difficulty band, for cross-system comparison.

YDS anchors difficultyScore at 2.5 points per letter rung
(5.0 = 20.0 ... 5.15d = 102.5). Other systems are placed on that axis from
published conversion charts; see docs/grade-axis.md.

Aid rows only have systemOrder. They intentionally leave difficultyScore and
confidence empty because aid grades do not map to free-climbing difficulty.
"""

import csv
import sys
from pathlib import Path

# All generated CSV rows are staged here first so validation can inspect the
# complete dataset before anything is written.
GRADE_ROWS = []


# difficultyScore is the shared project axis; keep it to two decimals in CSV.
def round_difficulty_score(score):
    return round(score + 1e-9, 2)


# systemOrder is only a within-system sort/rung value; keep it to three decimals.
def round_system_order(system_order):
    return round(system_order + 1e-9, 3)


# This is the only place a row is shaped for the CSV, so all callers use the
# same rounding and empty-value rules.
def add_grade_row(grade_system, discipline, raw_grade, system_order, difficulty_score, confidence, note):
    GRADE_ROWS.append({
        "gradeSystem": grade_system,
        "discipline": discipline,
        "rawGrade": raw_grade,
        "systemOrder": round_system_order(system_order),
        "difficultyScore": round_difficulty_score(difficulty_score) if difficulty_score is not None else "",
        "confidence": confidence if confidence is not None else "",
        "note": note,
    })


# ---------------------------------------------------------------- YDS (anchor)
# Letter-rung index: 5.0..5.9 -> 0..9; 5.10a=10 ... 5.15d=33.
def yds_letter_rung_index(yds_number, letter=None):
    # Below 5.10, the YDS minor number is already the rung index.
    if yds_number < 10:
        return float(yds_number)
    # From 5.10 up, each number has four letter rungs: a, b, c, d.
    return 10 + (yds_number - 10) * 4 + "abcd".index(letter)


def yds_difficulty_score(letter_rung_index):
    # YDS defines the project difficultyScore scale: 2.5 points per letter rung.
    return 20 + 2.5 * letter_rung_index


YDS_ANCHOR_NOTE = "axis anchor; consensus of MP/Wikipedia/ClimbingHouse/Sendy/Bergfreunde"

# systemOrder is a local YDS sort position; difficultyScore is the shared app axis.
# MP class strings below 5.0 appear in real alpine-route exports.
add_grade_row("YDS", "SPORT", "3rd", -3, 12.5, 0.8, "MP class string (scramble)")
add_grade_row("YDS", "SPORT", "4th", -2, 15.0, 0.8, "MP class string (exposed scramble)")
add_grade_row("YDS", "SPORT", "Easy 5th", -1, 17.5, 0.8, "MP class string (~5.0-5.2)")

for yds_minor in range(10):
    # Plain 5.0 through 5.9 are direct YDS anchor rungs.
    add_grade_row("YDS", "SPORT", f"5.{yds_minor}", yds_minor,
                  yds_difficulty_score(yds_minor), 1.0, YDS_ANCHOR_NOTE)
# sub-5.10 plus/minus variants confirmed in MP exports (5.7+, 5.8-, 5.8+, 5.9-, 5.9+)
for raw_grade, base_rung, one_third_direction in [
        ("5.7+", 7, +1), ("5.8-", 8, -1), ("5.8+", 8, +1),
        ("5.9-", 9, -1), ("5.9+", 9, +1)]:
    # These MP export variants sit one third of a YDS rung from the base grade.
    add_grade_row("YDS", "SPORT", raw_grade, base_rung + one_third_direction * 0.3,
                  yds_difficulty_score(base_rung + one_third_direction / 3), 0.9,
                  "MP-export +/- variant; one third of a rung")

for yds_number in range(10, 16):
    base_letter_rung = yds_letter_rung_index(yds_number, "a")
    for letter_index, letter in enumerate("abcd"):
        # Exact letter grades are the strongest YDS anchors on the shared axis.
        add_grade_row("YDS", "SPORT", f"5.{yds_number}{letter}", yds_number + letter_index * 0.25,
            yds_difficulty_score(base_letter_rung + letter_index), 1.0, YDS_ANCHOR_NOTE)
    # slash grades sit on the letter boundary (MP: 5.10a/b etc.)
    for letter_index, (low_letter, high_letter) in enumerate([("a", "b"), ("b", "c"), ("c", "d")]):
        add_grade_row("YDS", "SPORT", f"5.{yds_number}{low_letter}/{high_letter}",
            yds_number + letter_index * 0.25 + 0.125,
            yds_difficulty_score(base_letter_rung + letter_index + 0.5), 1.0,
            "letter-boundary slash grade (MP chart)")
    # coarse minus/plain/plus: live MP chart equates 5.N- = 5.Na, 5.N = 5.Nb/c, 5.N+ = 5.Nc/d
    add_grade_row("YDS", "SPORT", f"5.{yds_number}-", yds_number,
        yds_difficulty_score(base_letter_rung), 0.95,
        "pinned to 5.Na; sources split between 5.Na and 5.Na/b")
    add_grade_row("YDS", "SPORT", f"5.{yds_number}", yds_number + 0.375,
        yds_difficulty_score(base_letter_rung + 1.5), 0.95,
        "MP: plain 5.N = 5.Nb/c")
    add_grade_row("YDS", "SPORT", f"5.{yds_number}+", yds_number + 0.625,
        yds_difficulty_score(base_letter_rung + 2.5), 0.95,
        "MP: 5.N+ = 5.Nc/d")

# ------------------------------------------------------------------- FRENCH
# Values are difficultyScore on the shared project axis.
# French 6a..7a compresses nine YDS rungs into eight French rungs, so those
# grades use the midpoint between the MP and Wikipedia camps.
FRENCH_SCORE_BY_GRADE = {
    "1": 20.0, "1+": 21.25, "2": 22.5, "2+": 24.38, "3": 26.25, "3+": 27.5,
    "4a": 30.0, "4b": 32.5, "4c": 35.0,
    "4": 32.5, "4+": 36.88,                      # plain 4 covers 4a-4c ~ 4b
    "5a": 38.75, "5a+": 40.0, "5b": 41.25, "5b+": 42.5, "5c": 43.75, "5c+": 44.38,
    "5": 41.25, "5+": 44.38,                     # plain 5 ~ 5b; 5+ ~ 5c/6a
    "6a": 45.0, "6a+": 47.5, "6b": 50.0, "6b+": 52.5,
    "6c": 56.25, "6c+": 58.75, "7a": 61.25,      # compression-band midline
    "7a+": 65.0, "7b": 67.5, "7b+": 70.0, "7c": 72.5, "7c+": 75.0,
    "8a": 77.5, "8a+": 80.0, "8b": 82.5, "8b+": 85.0, "8c": 87.5, "8c+": 90.0,
    "9a": 92.5, "9a+": 95.0, "9b": 97.5, "9b+": 100.0, "9c": 102.5,
}
FRENCH_SLASH_LADDER = [
    "6a", "6a+", "6b", "6b+", "6c", "6c+", "7a", "7a+", "7b", "7b+",
    "7c", "7c+", "8a", "8a+", "8b", "8b+", "8c", "8c+",
    "9a", "9a+", "9b", "9b+", "9c",
]


# systemOrder is only for sorting inside the French grade ladder.
# "3+" is pinned to 3b's order (3.333): its score already equals 3b (27.5), and
# without the pin the letter formula would place it at 3.167 — colliding with
# the sub-4 letter row "3a+" at a different score, so ticks graded "3+" and
# "3a+" would sort as the same rung. Mirrors how plain "4"/"5" sit on their
# b-letter orders.
FRENCH_PLAIN_SYSTEM_ORDER = {"1+": 1.5, "2+": 2.5, "3+": 3.333, "4": 4.333, "4+": 4.833,
                             "5": 5.333, "5+": 5.833}


def french_system_order(raw_grade):
    # Plain French grades like "5" and "5+" cover wider bands than letter grades.
    if raw_grade in FRENCH_PLAIN_SYSTEM_ORDER:
        return FRENCH_PLAIN_SYSTEM_ORDER[raw_grade]
    # Letter grades sort as thirds of the number: 6a, 6b, 6c.
    grade_number = float(raw_grade[0])
    suffix = raw_grade[1:]
    if suffix.startswith(("a", "b", "c")):
        grade_number += "abc".index(suffix[0]) / 3
        suffix = suffix[1:]
    # A plus grade sorts halfway between this letter and the next letter.
    if suffix == "+":
        grade_number += 1 / 6
    return grade_number


def french_confidence(raw_grade):
    # The main 6a+ through 9c ladder has stronger source agreement than low grades.
    if raw_grade in FRENCH_SLASH_LADDER:
        return 1.0 if FRENCH_SCORE_BY_GRADE[raw_grade] >= 65 else 0.85
    return 0.7


for raw_grade, difficulty_score in FRENCH_SCORE_BY_GRADE.items():
    # Notes explain why the shared difficultyScore is trusted more or less here.
    note = ("universal 1:1 with YDS from 7a+=5.12a up (benchmark routes)"
            if FRENCH_SCORE_BY_GRADE[raw_grade] >= 65 else
            "compression band midline (MP vs Wikipedia camps)" if FRENCH_SCORE_BY_GRADE[raw_grade] >= 45 else
            "low-end alignment; sources vary a full rung")
    add_grade_row("FRENCH_SPORT", "SPORT", raw_grade, french_system_order(raw_grade),
                  difficulty_score, french_confidence(raw_grade), note)

# slash variants: 8a.nu short form "7a/+", full form "7a/7a+", boundary "6c+/7a"
for ladder_index in range(len(FRENCH_SLASH_LADDER) - 1):
    lower_grade = FRENCH_SLASH_LADDER[ladder_index]
    upper_grade = FRENCH_SLASH_LADDER[ladder_index + 1]
    # Slash grades are boundary rows: halfway in systemOrder and difficultyScore.
    midpoint_score = (FRENCH_SCORE_BY_GRADE[lower_grade] + FRENCH_SCORE_BY_GRADE[upper_grade]) / 2
    midpoint_order = (french_system_order(lower_grade) + french_system_order(upper_grade)) / 2
    confidence = min(french_confidence(lower_grade), french_confidence(upper_grade))
    if upper_grade == lower_grade + "+":
        add_grade_row("FRENCH_SPORT", "SPORT", f"{lower_grade}/+", midpoint_order, midpoint_score, confidence,
            "8a.nu short slash form")
        add_grade_row("FRENCH_SPORT", "SPORT", f"{lower_grade}/{upper_grade}",
            midpoint_order, midpoint_score, confidence,
            "full slash form")
    else:
        add_grade_row("FRENCH_SPORT", "SPORT", f"{lower_grade}/{upper_grade}",
            midpoint_order, midpoint_score, confidence,
            "letter-boundary slash form (8a.nu/logbooks)")

# Letter forms GradeParser accepts ([3-9][abc]+?) that the anchor table above
# doesn't name — without rows here they parse fine but resolve to no
# difficultyScore. Two gaps:
#   - sub-4 letters ("3a".."3c+", rare legacy/logbook forms): interpolated as
#     thirds between the 3 and 4a anchors, plus forms as midpoints. Note the
#     3b score lands exactly on the existing "3+" (27.5) — the plain plus and
#     the mid letter agree, same as "4"/"4b" and "5"/"5b" do — and "3+" is
#     order-pinned to 3b in FRENCH_PLAIN_SYSTEM_ORDER so the pair are exact
#     synonyms on both axes (not just the score one).
#   - sub-5 plus forms ("4a+".."4c+"): midpoints of the neighboring letter
#     rungs. "4c+" computes to the same order/score as the existing plain "4+"
#     (4.833 / 36.88), which is the fold identity ("4+" reads as ~4c+), so the
#     two rows are deliberate exact synonyms.
# These are emitted before the gym-route copy loop below, so FRENCH_SPORT/GYM
# rows for every form come free via the indoor curve.
third_of_3_to_4a = (FRENCH_SCORE_BY_GRADE["4a"] - FRENCH_SCORE_BY_GRADE["3"]) / 3
FRENCH_SUB_4_LETTER_ANCHORS = [
    ("3a", FRENCH_SCORE_BY_GRADE["3"], "sub-4 letter form (rare/legacy): synonym of 3"),
    ("3b", FRENCH_SCORE_BY_GRADE["3"] + third_of_3_to_4a,
     "sub-4 letter form (rare/legacy): interpolated third between 3 and 4a"),
    ("3c", FRENCH_SCORE_BY_GRADE["3"] + 2 * third_of_3_to_4a,
     "sub-4 letter form (rare/legacy): interpolated third between 3 and 4a"),
    # 4a closes the ladder for the 3c+ midpoint; sentinel only, not re-emitted.
    ("4a", FRENCH_SCORE_BY_GRADE["4a"], None),
]
for (raw_grade, difficulty_score, letter_note), (_, next_score, _) in zip(
        FRENCH_SUB_4_LETTER_ANCHORS, FRENCH_SUB_4_LETTER_ANCHORS[1:]):
    add_grade_row("FRENCH_SPORT", "SPORT", raw_grade, french_system_order(raw_grade),
                  difficulty_score, 0.7, letter_note)
    add_grade_row("FRENCH_SPORT", "SPORT", f"{raw_grade}+", french_system_order(f"{raw_grade}+"),
                  (difficulty_score + next_score) / 2, 0.7,
                  "sub-4 letter plus form (rare/legacy): midpoint of adjacent letter rungs")
for lower_grade, upper_grade in [("4a", "4b"), ("4b", "4c"), ("4c", "5a")]:
    add_grade_row("FRENCH_SPORT", "SPORT", f"{lower_grade}+", french_system_order(f"{lower_grade}+"),
                  (FRENCH_SCORE_BY_GRADE[lower_grade] + FRENCH_SCORE_BY_GRADE[upper_grade]) / 2, 0.7,
                  "letter plus form: midpoint of adjacent letter rungs")

# ---------------------------------------------------------- V_SCALE and FONT
# Values are difficultyScore. systemOrder stays the V number.
# BOULDER is the outdoor consensus line; GYM applies the indoor curve below.
OUTDOOR_V_SCORE_BY_GRADE = {
    -1: 41.25,                                   # VB ~ 5.8/5.9
    0: 46.25, 1: 51.25, 2: 56.25, 3: 61.25, 4: 65.0,
    5: 68.75, 6: 73.75, 7: 76.25, 8: 80.0, 9: 82.5,
    10: 85.0, 11: 87.5, 12: 90.0, 13: 92.5, 14: 95.0,
    15: 97.5, 16: 100.0, 17: 102.5,
}
# Indoor grading is a curve, not a flat shift: soft low, stiff high, and equal
# again at the top. Each tuple is (outdoor difficultyScore, indoor difficultyScore).
GYM_SCORE_CURVE = [
    (20.0, 19.0),      # ~5.0: trivial, near-identity
    (41.25, 36.25),    # VB / ~5.8-: soft
    (46.25, 40.0),     # V0 / 5.10a: soft (gym V0 ~ 5.8, 99Boulders)
    (56.25, 46.25),    # V2 / 5.11a-b: ~2 grades soft (AgentCalc)
    (65.0, 56.25),     # V4 / 5.12a: still soft
    (73.75, 65.0),     # V6 / 5.12d: closing
    (80.0, 74.0),      # V8 / 5.13c: nearly even
    (85.0, 84.0),      # V10 / 5.14a: CROSSOVER (indoor ~ outdoor here)
    (87.5, 89.0),      # V11 / 5.14b: indoor now STIFF
    (92.5, 96.0),      # V13 / 5.14d: ~1.5 grades stiff
    (97.5, 99.5),      # V15: stiff, easing
    (102.5, 102.5),    # V17 / 5.15d: converge (one hardest climb)
]


def indoor_score_for_outdoor_score(outdoor_score):
    # Clamp below the first control point to the first indoor score.
    curve = GYM_SCORE_CURVE
    if outdoor_score <= curve[0][0]:
        return curve[0][1]
    # Clamp above the last control point to the last indoor score.
    if outdoor_score >= curve[-1][0]:
        return curve[-1][1]
    for (outdoor_low, indoor_low), (outdoor_high, indoor_high) in zip(curve, curve[1:]):
        if outdoor_low <= outdoor_score <= outdoor_high:
            # Linearly interpolate between the two surrounding curve points.
            outdoor_span = outdoor_high - outdoor_low
            indoor_span = indoor_high - indoor_low
            return indoor_low + indoor_span * (outdoor_score - outdoor_low) / outdoor_span
    return outdoor_score


GYM_V_SCORE_BY_GRADE = {
    # Apply the indoor curve to every outdoor V difficultyScore.
    v_grade: indoor_score_for_outdoor_score(OUTDOOR_V_SCORE_BY_GRADE[v_grade])
    for v_grade in OUTDOOR_V_SCORE_BY_GRADE
}

# Slash forms are generated only for the Font grades that appear in real logs.
FONT_SLASH_LADDER = [
    "6A", "6A+", "6B", "6B+", "6C", "6C+", "7A", "7A+", "7B", "7B+",
    "7C", "7C+", "8A", "8A+", "8B", "8B+", "8C", "8C+", "9A",
]


def font_system_order(raw_grade):
    # Font grades sort like French, but with uppercase A/B/C and optional +/-.
    grade_number = float(raw_grade[0])
    suffix = raw_grade[1:]
    if suffix.startswith(("A", "B", "C")):
        grade_number += "ABC".index(suffix[0]) / 3
        suffix = suffix[1:]
    if suffix == "+":
        grade_number += 1 / 6
    if suffix == "-":
        grade_number -= 1 / 6
    return grade_number


def add_boulder_ladders(v_score_by_grade, discipline, confidence, v_note, font_note, low_font_scores):
    # Emit the V-scale rows first; Font rows are derived from these same scores.
    add_grade_row("V_SCALE", discipline, "VB", -1, v_score_by_grade[-1], confidence, "VB; " + v_note)
    add_grade_row("V_SCALE", discipline, "V-easy", -1, v_score_by_grade[-1],
                  confidence, "MP synonym for VB")
    for v_grade in range(0, 18):
        # The base V grade's systemOrder is the V number itself.
        add_grade_row("V_SCALE", discipline, f"V{v_grade}", v_grade,
                      v_score_by_grade[v_grade], confidence, v_note)
        # Minus grades sit one third of the way back toward the previous V grade.
        add_grade_row("V_SCALE", discipline, f"V{v_grade}-", v_grade - 0.25,
                      v_score_by_grade[v_grade] - (v_score_by_grade[v_grade] - v_score_by_grade[v_grade - 1]) / 3,
                      confidence, "minus variant: one third toward previous rung")
        if v_grade < 17:
            # Plus/range grades use the midpoint to the next V grade. This also
            # hits the published Font pins such as V3+=6A+ and V8+=7B+.
            midpoint_score = (v_score_by_grade[v_grade] + v_score_by_grade[v_grade + 1]) / 2
            add_grade_row("V_SCALE", discipline, f"V{v_grade}+", v_grade + 0.25,
                          midpoint_score, confidence,
                          "plus variant: midpoint to next rung (matches Font pins)")
            add_grade_row("V_SCALE", discipline, f"V{v_grade}-{v_grade + 1}", v_grade + 0.5,
                          midpoint_score, confidence,
                          "MP range grade: midpoint (synonym of Vn+)")

    # Font difficultyScore is derived from V so both boulder ladders agree.
    def midpoint_v_score(lower_v_grade, upper_v_grade):
        # A small helper keeps Font midpoint rows tied to the V source table.
        return (v_score_by_grade[lower_v_grade] + v_score_by_grade[upper_v_grade]) / 2

    # Values are difficultyScore. systemOrder is computed later from Font text.
    font_score_by_grade = {
        "1": low_font_scores[0], "2": low_font_scores[1], "3": v_score_by_grade[-1],
        "4-": v_score_by_grade[0] - (v_score_by_grade[0] - v_score_by_grade[-1]) / 3,
        "4": v_score_by_grade[0], "4+": midpoint_v_score(0, 1),
        "5-": v_score_by_grade[1] - (v_score_by_grade[1] - v_score_by_grade[0]) / 3,
        "5": v_score_by_grade[1], "5+": v_score_by_grade[2],
        "6A": v_score_by_grade[3], "6A+": midpoint_v_score(3, 4),
        "6B": v_score_by_grade[4], "6B+": midpoint_v_score(4, 5),
        "6C": v_score_by_grade[5], "6C+": midpoint_v_score(5, 6),
        "7A": v_score_by_grade[6], "7A+": v_score_by_grade[7],
        "7B": v_score_by_grade[8], "7B+": midpoint_v_score(8, 9),
        "7C": v_score_by_grade[9], "7C+": v_score_by_grade[10],
        "8A": v_score_by_grade[11], "8A+": v_score_by_grade[12],
        "8B": v_score_by_grade[13], "8B+": v_score_by_grade[14],
        "8C": v_score_by_grade[15], "8C+": v_score_by_grade[16],
        "9A": v_score_by_grade[17],
    }
    for raw_grade, difficulty_score in font_score_by_grade.items():
        # Emit base Font grades for this discipline: BOULDER or GYM.
        add_grade_row("FONT", discipline, raw_grade, font_system_order(raw_grade),
                      difficulty_score, confidence, font_note)

    # Sub-6A letter forms ("4a".."5c+"): 8a.nu exports and UK logbooks write low
    # Font boulders with French-style letters, and GradeParser resolves them to
    # FONT on boulders (uppercased by normalizeRawGrade), so every letter form
    # the parser accepts needs a row here or the tick silently gets no
    # difficultyScore. Canonical Font is numeric below 6A, so letters fold onto
    # the numeric rungs as synonyms: the low/mid/high letter thirds of a number
    # are its -/plain/+ triple (4A=4-, 4B=4, 4C=4+, 5A=5-, 5B=5, 5C=5+).
    # "3" has no numeric -/+ rungs, so 3A pins to 3 and 3B/3C interpolate
    # thirds toward 4-. Plus forms are midpoints of the neighboring letter
    # rungs, e.g. 5C+ lands halfway between 5+ (=5C) and 6A — matching how
    # climbers read it ("nearly 6A"). Synonym rows copy their target's
    # systemOrder as well as its score; giving letters their own bottom-anchored
    # third orders (4A=4.0, 4B=4.333...) would interleave wrongly with the
    # centered numeric triple (4B at 4.333 would sort after 4+ at 4.167 while
    # scoring below it, breaking monotonicity).
    third_of_3_to_4_minus = (font_score_by_grade["4-"] - font_score_by_grade["3"]) / 3
    sub_6a_note = "sub-6A letter form (8a.nu/UK logbook style)"
    sub_6a_letter_anchors = [
        ("3A", 3.0, font_score_by_grade["3"], f"{sub_6a_note}: synonym of Font 3"),
        ("3B", 3 + 1 / 3, font_score_by_grade["3"] + third_of_3_to_4_minus,
         f"{sub_6a_note}: interpolated third between 3 and 4- (no numeric 3+/3- rungs exist)"),
        ("3C", 3 + 2 / 3, font_score_by_grade["3"] + 2 * third_of_3_to_4_minus,
         f"{sub_6a_note}: interpolated third between 3 and 4- (no numeric 3+/3- rungs exist)"),
    ]
    for letter_grade, numeric_rung in [("4A", "4-"), ("4B", "4"), ("4C", "4+"),
                                       ("5A", "5-"), ("5B", "5"), ("5C", "5+")]:
        sub_6a_letter_anchors.append(
            (letter_grade, font_system_order(numeric_rung), font_score_by_grade[numeric_rung],
             f"{sub_6a_note}: synonym of Font {numeric_rung}"))
    # 6A closes the ladder so 5C+ gets a midpoint; it is a sentinel only and is
    # NOT re-emitted (the real 6A row already exists above).
    sub_6a_letter_anchors.append(("6A", font_system_order("6A"), font_score_by_grade["6A"], None))
    for (letter_grade, system_order, score, letter_note), (_, next_order, next_score, _) in zip(
            sub_6a_letter_anchors, sub_6a_letter_anchors[1:]):
        add_grade_row("FONT", discipline, letter_grade, system_order, score, confidence, letter_note)
        add_grade_row("FONT", discipline, f"{letter_grade}+",
                      (system_order + next_order) / 2, (score + next_score) / 2, confidence,
                      f"{sub_6a_note}: plus form, midpoint of adjacent letter rungs")

    for ladder_index in range(len(FONT_SLASH_LADDER) - 1):
        lower_grade = FONT_SLASH_LADDER[ladder_index]
        upper_grade = FONT_SLASH_LADDER[ladder_index + 1]
        # Font slash rows sit halfway between adjacent Font grades.
        midpoint_score = (font_score_by_grade[lower_grade] + font_score_by_grade[upper_grade]) / 2
        midpoint_order = (font_system_order(lower_grade) + font_system_order(upper_grade)) / 2
        if upper_grade == lower_grade + "+":
            add_grade_row("FONT", discipline, f"{lower_grade}/+", midpoint_order, midpoint_score, confidence,
                          "8a.nu short slash form")
            add_grade_row("FONT", discipline, f"{lower_grade}/{upper_grade}",
                          midpoint_order, midpoint_score, confidence,
                          "full slash form")
        else:
            add_grade_row("FONT", discipline, f"{lower_grade}/{upper_grade}",
                          midpoint_order, midpoint_score, confidence,
                          "letter-boundary slash form")


add_boulder_ladders(
    OUTDOOR_V_SCORE_BY_GRADE, "BOULDER", 0.5,
    "outdoor middle-line boulder-route bridge (opinion; spread +/-2 letters low, +/-1-2 high)",
    "outdoor; via near-unanimous V-Font crosswalk over the middle-line bridge",
    (32.5, 37.5))
add_boulder_ladders(
    GYM_V_SCORE_BY_GRADE, "GYM", 0.4,
    "indoor crossover: soft below ~V10, stiff above (comp/spray setting), converge at top; gyms vary widely",
    "indoor; via V-Font crosswalk over the gym crossover curve",
    (indoor_score_for_outdoor_score(32.5), indoor_score_for_outdoor_score(37.5)))

# ------------------------------------------------------------------- ICE_WI
# WI tracks the M scale rung-for-rung to ~6 (Wikipedia/AAC), M anchored by the
# 'M+4' rule to YDS. Rough movement-difficulty comparison only.
WATER_ICE_SCORE_BY_GRADE = {
    1: 32.5, 2: 35.0, 3: 37.5, 4: 40.0,
    5: 42.5, 6: 48.75, 7: 58.75, 8: 62.5,
}
WATER_ICE_NOTE = "via M-scale crosswalk + 'M+4' rule; rough movement comparison only"


def water_ice_confidence(ice_grade):
    # Confidence drops as WI grades move beyond the documented WI/M crosswalk.
    if ice_grade <= 6:
        return 0.3        # crosswalk validity documented up to ~WI6/M6
    if ice_grade == 7:
        return 0.25       # crosswalk past its stated validity
    return 0.2            # WI8: AAC 'under discussion', derived from disputed M8


water_ice_grades = sorted(WATER_ICE_SCORE_BY_GRADE)
for ice_grade in water_ice_grades:
    # Base WI rows use the grade number as systemOrder and the table value as difficultyScore.
    add_grade_row("ICE_WI", "ICE", f"WI{ice_grade}", ice_grade,
                  WATER_ICE_SCORE_BY_GRADE[ice_grade], water_ice_confidence(ice_grade), WATER_ICE_NOTE)
    if ice_grade > 1:
        # WI minus variants are one third of the way back toward the previous WI grade.
        previous_score = WATER_ICE_SCORE_BY_GRADE[ice_grade - 1]
        current_score = WATER_ICE_SCORE_BY_GRADE[ice_grade]
        add_grade_row("ICE_WI", "ICE", f"WI{ice_grade}-", ice_grade - 0.25,
                      current_score - (current_score - previous_score) / 3,
                      water_ice_confidence(ice_grade), "minus variant (rare)")
    if ice_grade < 8:
        # WI plus variants are one third of the way toward the next WI grade.
        note = ("plus variant (guidebook standard from WI3+)" if ice_grade >= 3
                else "plus variant (rare below WI3)")
        current_score = WATER_ICE_SCORE_BY_GRADE[ice_grade]
        next_score = WATER_ICE_SCORE_BY_GRADE[ice_grade + 1]
        add_grade_row("ICE_WI", "ICE", f"WI{ice_grade}+", ice_grade + 0.25,
                      current_score + (next_score - current_score) / 3,
                      water_ice_confidence(ice_grade), note)
        # MP range grades sit at the midpoint between adjacent WI grades.
        add_grade_row("ICE_WI", "ICE", f"WI{ice_grade}-{ice_grade + 1}", ice_grade + 0.5,
                      (current_score + next_score) / 2,
                      min(water_ice_confidence(ice_grade), water_ice_confidence(ice_grade + 1)),
                      "MP range grade")
# Helmcken Falls spray-ice extension: contested, off-ladder, near-zero confidence
HELMCKEN_SCORE_BY_GRADE = {10: 72.5, 11: 75.0, 12: 77.5, 13: 80.0}
for ice_grade, difficulty_score in HELMCKEN_SCORE_BY_GRADE.items():
    # Helmcken WI10-WI13 are real logged grades, but low-confidence off-ladder rows.
    add_grade_row("ICE_WI", "ICE", f"WI{ice_grade}", ice_grade, difficulty_score, 0.1,
        "Helmcken spray-ice extension; contested, WI8/9 skipped")
# WI10+ is placed one third of the way from WI10 toward WI11.
add_grade_row("ICE_WI", "ICE", "WI10+", 10.25,
              HELMCKEN_SCORE_BY_GRADE[10] + (HELMCKEN_SCORE_BY_GRADE[11] - HELMCKEN_SCORE_BY_GRADE[10]) / 3,
              0.1, "Helmcken spray-ice extension; contested")
# Alpine ice: same numerals; placed half a grade below same-numbered WI —
# the midline of AAC's 'about one grade softer' and regional no-offset usage.
# Orders offset by -0.4/-0.1 (not -0.5/0.0) so they never tie a WI order.
ALPINE_ICE_SCORE_BY_GRADE = {
    1: WATER_ICE_SCORE_BY_GRADE[1] - 1.25,
    2: 33.75, 3: 36.25, 4: 38.75, 5: 41.25,
    6: (WATER_ICE_SCORE_BY_GRADE[5] + WATER_ICE_SCORE_BY_GRADE[6]) / 2,
}
ALPINE_ICE_NOTE = ("half grade below same-numbered WI: midline of AAC 'about one grade"
                   " softer' vs regional no-offset usage")
for ice_grade in sorted(ALPINE_ICE_SCORE_BY_GRADE):
    # AI systemOrder is nudged below same-numbered WI so sorting stays stable.
    add_grade_row("ICE_WI", "ICE", f"AI{ice_grade}", ice_grade - 0.4,
                  ALPINE_ICE_SCORE_BY_GRADE[ice_grade], 0.25, ALPINE_ICE_NOTE)
    if ice_grade < 6:
        # AI range grades are midpoint rows between adjacent alpine-ice grades.
        add_grade_row("ICE_WI", "ICE", f"AI{ice_grade}-{ice_grade + 1}", ice_grade + 0.1,
                      (ALPINE_ICE_SCORE_BY_GRADE[ice_grade] + ALPINE_ICE_SCORE_BY_GRADE[ice_grade + 1]) / 2,
                      0.25, "MP range grade")

# ------------------------------------------------------------------- MIXED_M
MIXED_SCORE_BY_GRADE = {
    1: 32.5, 2: 35.0, 3: 37.5, 4: 40.0, 5: 42.5, 6: 48.75, 7: 58.75,
    8: 62.5, 9: 68.75, 10: 72.5, 11: 78.75, 12: 88.75,
    13: 91.25, 14: 93.75, 15: 96.25, 16: 98.75,
}


def mixed_confidence(mixed_grade):
    # Mixed-grade confidence drops as grades move away from the better-sourced M+4 rule.
    if mixed_grade <= 7:
        return 0.4
    if mixed_grade <= 12:
        return 0.25
    return 0.15


def mixed_note(mixed_grade):
    # Notes explain which source family controls this mixed difficultyScore band.
    if mixed_grade <= 7:
        return "'M+4' rule (M4~5.8): consensus of NEice/Blackbird/Ascentionism"
    if mixed_grade <= 12:
        return "disputed above M8 (Lowe 5.12 vs modern 5.11+); Blackbird extrapolation"
    return "elite rungs, highly subjective; M16 not established"


mixed_grades = sorted(MIXED_SCORE_BY_GRADE)
for mixed_grade in mixed_grades:
    # Base mixed rows use the M number as systemOrder and the table value as difficultyScore.
    add_grade_row("MIXED_M", "MIXED", f"M{mixed_grade}", mixed_grade,
                  MIXED_SCORE_BY_GRADE[mixed_grade],
                  mixed_confidence(mixed_grade), mixed_note(mixed_grade))
    if mixed_grade > 1:
        # M minus variants are one third of the way back toward the previous M grade.
        current_score = MIXED_SCORE_BY_GRADE[mixed_grade]
        previous_score = MIXED_SCORE_BY_GRADE[mixed_grade - 1]
        add_grade_row("MIXED_M", "MIXED", f"M{mixed_grade}-", mixed_grade - 0.25,
                      current_score - (current_score - previous_score) / 3,
                      min(mixed_confidence(mixed_grade), mixed_confidence(mixed_grade - 1)),
                      "minus variant")
    if mixed_grade < 16:
        # M plus variants move one third toward the next grade; ranges use the midpoint.
        current_score = MIXED_SCORE_BY_GRADE[mixed_grade]
        next_score = MIXED_SCORE_BY_GRADE[mixed_grade + 1]
        add_grade_row("MIXED_M", "MIXED", f"M{mixed_grade}+", mixed_grade + 0.25,
                      current_score + (next_score - current_score) / 3,
                      min(mixed_confidence(mixed_grade), mixed_confidence(mixed_grade + 1)),
                      "plus variant")
        add_grade_row("MIXED_M", "MIXED", f"M{mixed_grade}-{mixed_grade + 1}", mixed_grade + 0.5,
                      (current_score + next_score) / 2,
                      min(mixed_confidence(mixed_grade), mixed_confidence(mixed_grade + 1)),
                      "MP range grade")

# ------------------------------------------------------------------- E_Grade
# Rockfax/UKC-style E-to-French line for well-protected routes. Top end
# COMPRESSES to the real-route anchors — E10 ~ 8c+ (Equilibrium/Meltdown-era),
# E11 ~ 8c+/9a (Rhapsody, Lexicon), E12 ~ 9a (Bon Voyage) — rather than
# extending the linear slope into 5.15 (MP's table-filling artifact, rejected).
BRITISH_E_SCORE_BY_GRADE = {
    1: 46.25, 2: 50.0, 3: 56.25, 4: 61.25, 5: 66.25, 6: 71.25,
    7: 76.25, 8: 81.25, 9: 86.25, 10: 90.0, 11: 91.25, 12: 92.5,
}
BRITISH_E_NOTE = "Rockfax-style E-to-French (well-protected reading); danger skews +/-2 E per tier"


def british_e_confidence(e_grade):
    # E-grade confidence is highest where charts agree and lower at the route-anchored top.
    if e_grade <= 4:
        return 0.6        # sources agree within one French letter
    if e_grade <= 9:
        return 0.5        # documented ~2-letter divergence between camps
    return 0.4            # single-route anchored top rungs


for e_grade in sorted(BRITISH_E_SCORE_BY_GRADE):
    # Base E rows use the E number as systemOrder and the table value as difficultyScore.
    note = BRITISH_E_NOTE if e_grade < 10 else BRITISH_E_NOTE + "; anchored by real routes, compressed top"
    add_grade_row("E_Grade", "TRAD", f"E{e_grade}", e_grade,
                  BRITISH_E_SCORE_BY_GRADE[e_grade], british_e_confidence(e_grade), note)
    if e_grade < 12:
        # UKC slash rows sit halfway between neighboring E grades.
        add_grade_row("E_Grade", "TRAD", f"E{e_grade}/{e_grade + 1}", e_grade + 0.5,
                      (BRITISH_E_SCORE_BY_GRADE[e_grade] + BRITISH_E_SCORE_BY_GRADE[e_grade + 1]) / 2,
                      min(british_e_confidence(e_grade), british_e_confidence(e_grade + 1)),
                      "UKC logbook slash rung")
# canonical logged 'E<n> <tech>' pairings, incl. attested off-typical forms
# (E1 5a bold / E1 5c safe-cruxy, MP's E2 5b, E9 6c, E10 7c)
BRITISH_TECH_GRADES_BY_E_GRADE = {
    1: ["5a", "5b", "5c"], 2: ["5b", "5c"], 3: ["5c", "6a"],
    4: ["6a", "6b"], 5: ["6a", "6b"], 6: ["6b", "6c"], 7: ["6b", "6c"],
    8: ["6c", "7a"], 9: ["6c", "7a", "7b"], 10: ["7a", "7c"],
    11: ["7a"], 12: ["7a"],
}
for e_grade, tech_grades in BRITISH_TECH_GRADES_BY_E_GRADE.items():
    for tech_grade in tech_grades:
        # E-grade plus British-tech strings are synonyms of the bare E grade here.
        add_grade_row("E_Grade", "TRAD", f"E{e_grade} {tech_grade}", e_grade,
                      BRITISH_E_SCORE_BY_GRADE[e_grade], british_e_confidence(e_grade),
                      "canonical logged form; scored as bare E rung")
# sub-E adjectival ladder (legacy logged data); negative systemOrder below E1
SUB_E_SCORE_BY_GRADE = [
    ("Mod", 21.25), ("Moderate", 21.25), ("M", 21.25),
    ("Diff", 23.75), ("D", 23.75), ("HD", 26.25),
    ("VDiff", 28.75), ("VD", 28.75), ("HVD", 31.25), ("MS", 32.5),
    ("Severe", 33.75), ("Sev", 33.75), ("S", 33.75), ("HS", 36.25),
    ("MVS", 37.5), ("VS", 38.75), ("HVS", 41.25),
]
SUB_E_SYSTEM_ORDER = {
    "Mod": -11, "Moderate": -11, "M": -11, "Diff": -10, "D": -10,
    "HD": -9, "VDiff": -8, "VD": -8, "HVD": -7, "MS": -6,
    "Severe": -5, "Sev": -5, "S": -5, "HS": -4, "MVS": -3,
    "VS": -2, "HVS": -1,
}
for raw_grade, difficulty_score in SUB_E_SCORE_BY_GRADE:
    # Sub-E adjectival grades sit below E1 using negative systemOrder values.
    add_grade_row("E_Grade", "TRAD", raw_grade, SUB_E_SYSTEM_ORDER[raw_grade],
                  difficulty_score, 0.55,
                  "UK adjectival ladder below E1 (BMC/MP alignment)")

# ----------------------------------------------------------------------- AID
# No published source places aid grades on a free-climbing difficulty axis
# (UIAA, AAJ, MP, Wikipedia all keep it separate: risk/time, not gymnastics).
# systemOrder only; difficultyScore and confidence deliberately empty.
AID_NOTE = "risk/time axis; NO published free-climbing equivalence exists - ordered within-system only"
for prefix in ("A", "C"):
    for aid_grade in range(6):
        # Aid grades are ordered inside AID only; no free-climbing difficultyScore.
        add_grade_row("AID", "AID", f"{prefix}{aid_grade}", aid_grade, None, None, AID_NOTE)
        if aid_grade >= 2:
            # New-wave plus aid grades sort one quarter rung after the base grade.
            add_grade_row("AID", "AID", f"{prefix}{aid_grade}+", aid_grade + 0.25, None, None,
                AID_NOTE + "; new-wave plus rung")
        # MP range grades (A0-1 ... A5-6, C0-1 ... C4-5) are attested export strings
        if aid_grade < 5 or prefix == "A":
            add_grade_row("AID", "AID", f"{prefix}{aid_grade}-{aid_grade + 1}", aid_grade + 0.5, None, None,
                AID_NOTE + "; MP range grade")
add_grade_row("AID", "AID", "A6", 5.25, None, None,
    AID_NOTE + "; new-wave synonym for UIAA A5+ (same rung)")

# -------------------------------------------- indoor gym ladders for ROUTES
# Routes get the SAME crossover curve as boulders (soft low, stiff high): the
# sources chart no gym-rope scale, but the boulder gym/outdoor crossover happens
# at difficultyScore 85 (~V10 / 5.14a), which maps cleanly onto routes.
# Low confidence (0.4): this is a modeled offset, not sourced directly.
GYM_ROUTE_NOTE = ("indoor crossover: soft below ~5.14a, stiff above (comp/board"
                  " setting), converge at top; modeled offset, gyms vary")
gym_route_rows = []
for outdoor_route_row in GRADE_ROWS:
    if outdoor_route_row["gradeSystem"] in ("YDS", "FRENCH_SPORT") and outdoor_route_row["discipline"] == "SPORT":
        # Copy outdoor route rows into GYM discipline, changing only the score, confidence, and note.
        gym_route_rows.append({
            "gradeSystem": outdoor_route_row["gradeSystem"],
            "discipline": "GYM",
            "rawGrade": outdoor_route_row["rawGrade"],
            "systemOrder": outdoor_route_row["systemOrder"],
            "difficultyScore": round_difficulty_score(
                indoor_score_for_outdoor_score(outdoor_route_row["difficultyScore"])),
            "confidence": 0.4,
            "note": GYM_ROUTE_NOTE,
        })
GRADE_ROWS.extend(gym_route_rows)

# ------------------------------------------------------------------ validate
validation_errors = []
rows_by_natural_key = {}
for row in GRADE_ROWS:
    # Natural key must be unique because runtime lookups use this exact tuple.
    key = (row["gradeSystem"], row["discipline"], row["rawGrade"])
    if key in rows_by_natural_key:
        validation_errors.append(f"DUPLICATE natural key: {key}")
    rows_by_natural_key[key] = row

rows_by_system_and_discipline = {}
for row in GRADE_ROWS:
    # Group rows so each system/discipline ladder can be checked independently.
    rows_by_system_and_discipline.setdefault((row["gradeSystem"], row["discipline"]), []).append(row)

for (grade_system, discipline), rows in rows_by_system_and_discipline.items():
    # Aid rows are skipped here because empty difficultyScore means "not on the shared axis."
    scored_rows = [row for row in rows if row["difficultyScore"] != ""]
    scored_rows.sort(key=lambda row: (row["systemOrder"], row["difficultyScore"]))
    for previous_row, current_row in zip(scored_rows, scored_rows[1:]):
        # difficultyScore must never decrease as systemOrder increases.
        if current_row["difficultyScore"] < previous_row["difficultyScore"] - 1e-9:
            validation_errors.append(
                f"NON-MONOTONIC {grade_system}/{discipline}: "
                f"{previous_row['rawGrade']}(order {previous_row['systemOrder']},"
                f" score {previous_row['difficultyScore']}) -> {current_row['rawGrade']}"
                f"(order {current_row['systemOrder']}, score {current_row['difficultyScore']})")

DEFAULT_DISCIPLINE = {"YDS": "SPORT", "FRENCH_SPORT": "SPORT", "V_SCALE": "BOULDER",
                      "FONT": "BOULDER", "E_Grade": "TRAD"}


def difficulty_score_for(grade_system, raw_grade, discipline=None):
    # Test helpers default to the app's normal discipline for each grade system.
    default_discipline = discipline or DEFAULT_DISCIPLINE[grade_system]
    return rows_by_natural_key[(grade_system, default_discipline, raw_grade)]["difficultyScore"]

ANCHOR_CHECKS = [
    ("5.12a = 7a+ (universal)", difficulty_score_for("YDS", "5.12a"), difficulty_score_for("FRENCH_SPORT", "7a+")),
    ("V4 = 5.12a (strongest boulder-route agreement)",
     difficulty_score_for("V_SCALE", "V4"), difficulty_score_for("YDS", "5.12a")),
    ("V6 = 7A (universal boulder anchor)", difficulty_score_for("V_SCALE", "V6"), difficulty_score_for("FONT", "7A")),
    ("V17 = 9A = 5.15d = 9c (world-top convergence)",
     difficulty_score_for("V_SCALE", "V17"), difficulty_score_for("FONT", "9A")),
    ("V17 = 5.15d", difficulty_score_for("V_SCALE", "V17"), difficulty_score_for("YDS", "5.15d")),
    ("9c = 5.15d", difficulty_score_for("FRENCH_SPORT", "9c"), difficulty_score_for("YDS", "5.15d")),
    ("5.14d = 9a (Action Directe)", difficulty_score_for("YDS", "5.14d"), difficulty_score_for("FRENCH_SPORT", "9a")),
    ("5.10a = 6a (majority camp)", difficulty_score_for("YDS", "5.10a"), difficulty_score_for("FRENCH_SPORT", "6a")),
    ("V3+ = 6A+ (published Font pin)", difficulty_score_for("V_SCALE", "V3+"), difficulty_score_for("FONT", "6A+")),
    ("V8+ = 7B+ (published Font pin)", difficulty_score_for("V_SCALE", "V8+"), difficulty_score_for("FONT", "7B+")),
    ("E12 = 9a (Bon Voyage)", difficulty_score_for("E_Grade", "E12"), difficulty_score_for("FRENCH_SPORT", "9a")),
    ("E11 between 8c+ and 9a (Rhapsody)", difficulty_score_for("E_Grade", "E11"),
     (difficulty_score_for("FRENCH_SPORT", "8c+") + difficulty_score_for("FRENCH_SPORT", "9a")) / 2),
    ("gym V0 = 5.8 (soft low end, 99Boulders gym pin)",
     difficulty_score_for("V_SCALE", "V0", "GYM"), difficulty_score_for("YDS", "5.8")),
    ("gym V2 = outdoor V0 (soft low end)",
     difficulty_score_for("V_SCALE", "V2", "GYM"), difficulty_score_for("V_SCALE", "V0")),
    ("gym Font 7A = gym V6 (crosswalk holds indoors)",
     difficulty_score_for("FONT", "7A", "GYM"), difficulty_score_for("V_SCALE", "V6", "GYM")),
]
for check_name, left_score, right_score in ANCHOR_CHECKS:
    # Anchor identities guard the most important cross-system equivalences.
    if abs(left_score - right_score) > 1e-9:
        validation_errors.append(f"ANCHOR BROKEN: {check_name}: {left_score} != {right_score}")

# Crossover checks keep gym rows softer low, stiffer high, and equal at the top.
GYM_CROSSOVER_CHECKS = [
    ("boulder soft: gym V4 < outdoor V4",
     difficulty_score_for("V_SCALE", "V4", "GYM"), difficulty_score_for("V_SCALE", "V4"), "<"),
    ("boulder stiff: gym V13 > outdoor V13",
     difficulty_score_for("V_SCALE", "V13", "GYM"), difficulty_score_for("V_SCALE", "V13"), ">"),
    ("boulder converge: gym V17 = outdoor V17 (one hardest)",
     difficulty_score_for("V_SCALE", "V17", "GYM"), difficulty_score_for("V_SCALE", "V17"), "="),
    ("route soft: gym 5.10a < outdoor 5.10a",
     difficulty_score_for("YDS", "5.10a", "GYM"), difficulty_score_for("YDS", "5.10a"), "<"),
    ("route stiff: gym 5.14b > outdoor 5.14b",
     difficulty_score_for("YDS", "5.14b", "GYM"), difficulty_score_for("YDS", "5.14b"), ">"),
    ("route soft: gym 7a < outdoor 7a (French)",
     difficulty_score_for("FRENCH_SPORT", "7a", "GYM"), difficulty_score_for("FRENCH_SPORT", "7a"), "<"),
    ("route stiff: gym 8c > outdoor 8c (French)",
     difficulty_score_for("FRENCH_SPORT", "8c", "GYM"), difficulty_score_for("FRENCH_SPORT", "8c"), ">"),
]
for check_name, left_score, right_score, operator in GYM_CROSSOVER_CHECKS:
    # Each check stores the comparison operator as data so failures print clearly.
    ok = (
        (left_score < right_score - 1e-9) if operator == "<"
        else (left_score > right_score + 1e-9) if operator == ">"
        else abs(left_score - right_score) < 1e-9
    )
    if not ok:
        validation_errors.append(
            f"CROSSOVER BROKEN: {check_name}: {left_score} {operator} {right_score} is false")

if validation_errors:
    print("VALIDATION FAILED:")
    for validation_error in validation_errors:
        print(" ", validation_error)
    sys.exit(1)

# --------------------------------------------------- coverage vs real MP data
ticks_csv = Path("inputs/ticks.csv")
if not ticks_csv.exists():
    ticks_csv = Path("../inputs/ticks.csv")
if ticks_csv.exists():
    raw_ratings = set()
    with open(ticks_csv, encoding="utf-8-sig", newline="") as f:
        for tick_row in csv.DictReader(f):
            # Mountain Project exports store the displayed grade in the Rating column.
            rating = (tick_row.get("Rating") or "").strip()
            if rating:
                raw_ratings.add(rating)
    known_raw_grades = {row["rawGrade"] for row in GRADE_ROWS}
    unmatched_ratings = []
    for rating in sorted(raw_ratings):
        # Protection suffixes are not seeded because the parser strips them before lookup.
        rating_without_suffix = rating.split()[0]          # strip protection suffixes (PG13/R/X)
        if rating not in known_raw_grades and rating_without_suffix not in known_raw_grades:
            unmatched_ratings.append(rating)
    print(f"coverage vs {ticks_csv}: {len(raw_ratings)} distinct ratings,"
          f" {len(raw_ratings) - len(unmatched_ratings)} covered, unmatched: {unmatched_ratings}")
else:
    print("inputs/ticks.csv not found - skipped real-data coverage check")

# -------------------------------------------------------------------- output
output_csv = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("grade-mappings.csv")
output_csv.parent.mkdir(parents=True, exist_ok=True)
with open(output_csv, "w", newline="", encoding="utf-8") as f:
    # Keep field order stable because the CSV is checked into app resources.
    writer = csv.DictWriter(f, fieldnames=["gradeSystem", "discipline", "rawGrade",
                                           "systemOrder", "difficultyScore",
                                           "confidence", "note"])
    writer.writeheader()
    grade_system_sort_order = {"YDS": 0, "FRENCH_SPORT": 1, "V_SCALE": 2, "FONT": 3,
                               "ICE_WI": 4, "MIXED_M": 5, "E_Grade": 6, "AID": 7}
    for row in sorted(GRADE_ROWS, key=lambda r: (grade_system_sort_order[r["gradeSystem"]],
                                                r["discipline"],
                                                r["systemOrder"],
                                                r["difficultyScore"] or 0)):
        # Sort by grade system, discipline, systemOrder, then difficultyScore for stable diffs.
        writer.writerow(row)

print(f"wrote {len(GRADE_ROWS)} rows to {output_csv}")
for key in sorted(rows_by_system_and_discipline, key=lambda k: len(rows_by_system_and_discipline[k])):
    print(f"  {key[0]}/{key[1]}: {len(rows_by_system_and_discipline[key])}")
print("all validation checks passed")
