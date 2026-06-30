export type SendFormValues = {
  climbName: string;
  climbId: string;
  location: string;
  discipline: string;
  grade: string;
  gradeSystem: string;
  sourceApp: string;
  externalId: string;
  sourceUrl: string;
  sendDate: string;
  style: string;
  ropeSendStyle: string;
  attempts: string;
  notes: string;
};

export type SendPayload = {
  climbName: string;
  climbId: string | null;
  location: string | null;
  discipline: string;
  grade: string | null;
  gradeSystem: string;
  sourceApp: string;
  externalId: string | null;
  sourceUrl: string | null;
  sendDate: string | null;
  style: string | null;
  ropeSendStyle: string;
  attempts: number | null;
  notes: string | null;
};

export type SendRecord = Omit<SendPayload, "attempts"> & {
  id: number;
  attempts: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export const DEFAULT_SEND_VALUES: SendFormValues = {
  climbName: "",
  climbId: "",
  location: "",
  discipline: "UNKNOWN",
  grade: "",
  gradeSystem: "UNKNOWN",
  sourceApp: "MANUAL",
  externalId: "",
  sourceUrl: "",
  sendDate: "",
  style: "",
  ropeSendStyle: "UNKNOWN",
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

export const ROPE_SEND_STYLE_OPTIONS = [
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

export function toSendPayload(values: SendFormValues): SendPayload {
  const attempts = Number(values.attempts);

  return {
    climbName: values.climbName.trim(),
    climbId: optionalText(values.climbId),
    location: optionalText(values.location),
    discipline: values.discipline,
    grade: optionalText(values.grade),
    gradeSystem: values.gradeSystem,
    sourceApp: values.sourceApp,
    externalId: optionalText(values.externalId),
    sourceUrl: optionalText(values.sourceUrl),
    sendDate: optionalText(values.sendDate),
    style: optionalText(values.style),
    ropeSendStyle: values.ropeSendStyle,
    attempts: Number.isFinite(attempts) && values.attempts.trim() !== "" ? attempts : null,
    notes: optionalText(values.notes),
  };
}

function optionalText(value: string) {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}
