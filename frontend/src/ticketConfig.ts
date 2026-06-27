export type TicketFormValues = {
  title: string;
  description: string;
  status: string;
  priority: string;
  assignee: string;
};

export const DEFAULT_TICKET_VALUES: TicketFormValues = {
  title: "",
  description: "",
  status: "OPEN",
  priority: "LOW",
  assignee: "",
};

export const STATUS_OPTIONS = [
  { value: "OPEN", label: "Open", tone: "open", icon: "X" },
  { value: "IN_PROGRESS", label: "In Progress", tone: "progress", icon: "!" },
  { value: "IN_REVIEW", label: "In Review", tone: "review", icon: "•" },
  { value: "CLOSED", label: "Done", tone: "done", icon: "✓" },
] as const;

export const PRIORITY_OPTIONS = [
  { value: "LOW", label: "Low" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HIGH", label: "High" },
  { value: "CRITICAL", label: "Critical" },
] as const;

export const FILTER_OPTIONS = {
  status: [{ value: "ALL", label: "All Statuses" }, ...STATUS_OPTIONS],
  priority: [{ value: "ALL", label: "All Priorities" }, ...PRIORITY_OPTIONS],
};

export function formatEnumLabel(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function getStatusMeta(status: string) {
  return (
    STATUS_OPTIONS.find((option) => option.value === status) ?? {
      value: status,
      label: formatEnumLabel(status),
      tone: "neutral",
      icon: "•",
    }
  );
}
