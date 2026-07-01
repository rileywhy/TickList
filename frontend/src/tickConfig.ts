export type TickFormValues = {
  climbName: string;
  climbId: string;
  location: string;
  discipline: string;
  grade: string;
  gradeSystem: string;
  sourceApp: string;
  tickType: string;
  externalId: string;
  sourceUrl: string;
  tickDate: string;
  style: string;
  ropeStyle: string;
  attempts: string;
  notes: string;
};

export type TickPayload = {
  climbName: string;
  climbId: string | null;
  location: string | null;
  discipline: string;
  grade: string | null;
  gradeSystem: string;
  sourceApp: string;
  tickType: string;
  externalId: string | null;
  sourceUrl: string | null;
  tickDate: string | null;
  style: string | null;
  ropeStyle: string;
  attempts: number | null;
  notes: string | null;
};

export type TickRecord = Omit<TickPayload, "attempts"> & {
  id: number;
  attempts: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export const DEFAULT_TICK_VALUES: TickFormValues = {
  climbName: "",
  climbId: "",
  location: "",
  discipline: "UNKNOWN",
  grade: "",
  gradeSystem: "UNKNOWN",
  sourceApp: "MANUAL",
  tickType: "SEND",
  externalId: "",
  sourceUrl: "",
  tickDate: "",
  style: "",
  ropeStyle: "UNKNOWN",
  attempts: "",
  notes: "",
};

export const GRADE_SYSTEM_OPTIONS = [
  { value: "UNKNOWN", label: "Unknown" },
  { value: "V_SCALE", label: "V Scale" },
  { value: "FONT", label: "Font" },
  { value: "YDS", label: "YDS" },
  { value: "FRENCH_SPORT", label: "French Sport" },
  { value: "ICE_WI", label: "Ice WI" },
  { value: "MIXED_M", label: "Mixed M" },
  { value: "AID", label: "Aid" },
  { value: "E_Grade", label: "E Grade" },
] as const;

export const TICK_TYPE_OPTIONS = [
  { value: "SEND", label: "Send" },
  { value: "ATTEMPT", label: "Attempt" },
  { value: "CLEAN_TR", label: "Clean TR" },
  { value: "UNKNOWN", label: "Unknown" },
] as const;

export const SOURCE_APP_OPTIONS = [
  { value: "MANUAL", label: "Manual", tone: "manual", icon: "M" },
  {
    value: "MOUNTAIN_PROJECT",
    label: "Mountain Project",
    tone: "mountain-project",
    icon: "MP",
  },
  { value: "KAYA", label: "Kaya", tone: "kaya", icon: "K" },
  { value: "EIGHT_A", label: "8a", tone: "eight-a", icon: "8a" },
  { value: "UNKNOWN", label: "Unknown", tone: "unknown", icon: "?" },
] as const;

export const DISCIPLINE_OPTIONS = [
  { value: "UNKNOWN", label: "Unknown" },
  { value: "BOULDER", label: "Boulder" },
  { value: "SPORT", label: "Sport" },
  { value: "TRAD", label: "Trad" },
  { value: "ICE", label: "Ice" },
  { value: "MIXED", label: "Mixed" },
  { value: "AID", label: "Aid" },
  { value: "GYM", label: "Gym" },
] as const;

export const ROPE_STYLE_OPTIONS = [
  { value: "UNKNOWN", label: "Unknown" },
  { value: "FLASH", label: "Flash" },
  { value: "ONSIGHT", label: "Onsight" },
  { value: "REDPOINT", label: "Redpoint" },
  { value: "PINKPOINT", label: "Pinkpoint" },
  { value: "REPEAT", label: "Repeat" },
  { value: "SOLO", label: "Solo" },
  { value: "LRS", label: "Lead rope solo" },
  { value: "FS", label: "Free solo" },
  { value: "OSFS", label: "Onsight free solo" },
  { value: "DS", label: "Daisy solo" },
  { value: "FB", label: "Freebase" },
  { value: "DWS", label: "Deep water solo" },
] as const;

export const FILTER_OPTIONS = {
  sourceApp: [{ value: "ALL", label: "All Sources" }, ...SOURCE_APP_OPTIONS],
  gradeSystem: [{ value: "ALL", label: "All Grade Systems" }, ...GRADE_SYSTEM_OPTIONS],
};

export function formatEnumLabel(value: string | null | undefined) {
  if (!value) {
    return "Unknown";
  }

  return value
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function getSourceMeta(sourceApp: string | null | undefined) {
  return (
    SOURCE_APP_OPTIONS.find((option) => option.value === sourceApp) ?? {
      value: sourceApp ?? "UNKNOWN",
      label: formatEnumLabel(sourceApp),
      tone: "unknown",
      icon: "?",
    }
  );
}

export function toTickPayload(values: TickFormValues): TickPayload {
  const attempts = Number(values.attempts);

  return {
    climbName: values.climbName.trim(),
    climbId: optionalText(values.climbId),
    location: optionalText(values.location),
    discipline: values.discipline,
    grade: optionalText(values.grade),
    gradeSystem: values.gradeSystem,
    sourceApp: values.sourceApp,
    tickType: values.tickType,
    externalId: optionalText(values.externalId),
    sourceUrl: optionalText(values.sourceUrl),
    tickDate: optionalText(values.tickDate),
    style: optionalText(values.style),
    ropeStyle: values.ropeStyle,
    attempts: Number.isFinite(attempts) && values.attempts.trim() !== "" ? attempts : null,
    notes: optionalText(values.notes),
  };
}

function optionalText(value: string) {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}
