import { useState, type ReactNode } from "react";
import TickForm from "./TickForm";
import {
  formatEnumLabel,
  getSourceMeta,
  toTickPayload,
  type TickFormValues,
  type TickRecord,
} from "../tickConfig";

type TickCardProps = {
  tick: TickRecord;
  onAuthExpired: () => void;
  onDelete: () => Promise<void>;
  onUpdate: () => Promise<void>;
  token: string;
};

function TickCard({
  tick,
  onAuthExpired,
  onDelete,
  onUpdate,
  token,
}: TickCardProps) {
  const [showDetails, setShowDetails] = useState(false);
  const [editing, setEditing] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const sourceMeta = getSourceMeta(tick.sourceApp);
  const gradeLabel = tick.grade
    ? `${tick.grade} (${formatEnumLabel(tick.gradeSystem)})`
    : formatEnumLabel(tick.gradeSystem);
  const details = [
    ["Location", tick.location || "Unknown"],
    ["Grade", gradeLabel],
    ["Discipline", formatEnumLabel(tick.discipline)],
    ["Source", sourceMeta.label],
    ["Type", formatEnumLabel(tick.tickType)],
    ["Tick style", tick.style || "Unknown"],
    ["Rope style", formatEnumLabel(tick.ropeStyle)],
    ["Tick date", formatDate(tick.tickDate)],
    ["Attempts", tick.attempts?.toString() ?? "Unknown"],
    ["External ID", tick.externalId || "None"],
    ["Climb ID", tick.climbId || "None"],
    ["Created", formatDateTime(tick.createdAt)],
    ["Updated", formatDateTime(tick.updatedAt)],
  ];

  return (
    <div className="tick-card tick-row">
      <div className="tick-row__main">
        <div className="tick-row__header">
          <div className="tick-row__title-group">
            <h3 className="tick-row__title">{tick.climbName}</h3>
            <span className={`tick-source tick-source--${sourceMeta.tone}`}>
              <span className="tick-source__icon" aria-hidden="true">
                {sourceMeta.icon}
              </span>
              <span>{sourceMeta.label}</span>
            </span>
          </div>

          <div className="tick-row__actions">
            <ActionButton
              label="Show Details"
              onClick={() => setShowDetails(true)}
            >
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M12 5c5.23 0 9.27 4.1 10.7 6.02a1.6 1.6 0 0 1 0 1.96C21.27 14.9 17.23 19 12 19S2.73 14.9 1.3 12.98a1.6 1.6 0 0 1 0-1.96C2.73 9.1 6.77 5 12 5Zm0 2C8.3 7 5.23 9.73 3.42 12 5.23 14.27 8.3 17 12 17s6.77-2.73 8.58-5C18.77 9.73 15.7 7 12 7Zm0 2.25A2.75 2.75 0 1 1 9.25 12 2.75 2.75 0 0 1 12 9.25Z" />
              </svg>
            </ActionButton>

            {!editing && (
              <ActionButton
                label="Edit tick"
                onClick={() => {
                  setDeleteError("");
                  setEditing(true);
                }}
              >
                <svg aria-hidden="true" viewBox="0 0 24 24">
                  <path d="m16.86 3.49 3.65 3.65a1.5 1.5 0 0 1 0 2.12l-9.9 9.9-4.95 1.3 1.3-4.95 9.9-9.9a1.5 1.5 0 0 1 2.12 0ZM8.1 16.4l-.55 2.05 2.05-.55 8.97-8.97-1.5-1.5L8.1 16.4Z" />
                </svg>
              </ActionButton>
            )}

            <ActionButton
              danger
              disabled={isDeleting}
              label={isDeleting ? "Deleting tick" : "Delete tick"}
              onClick={deleteTick}
            >
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M6.7 5.3 12 10.59l5.3-5.3 1.4 1.41L13.41 12l5.3 5.29-1.41 1.41L12 13.41l-5.29 5.3-1.41-1.42L10.59 12 5.3 6.71 6.7 5.3Z" />
              </svg>
            </ActionButton>
          </div>
        </div>

        {tick.notes && <p className="tick-row__description">{tick.notes}</p>}
        {deleteError && <p role="alert">{deleteError}</p>}

        <div className="tick-row__meta">
          {details.slice(0, 7).map(([label, value]) => (
            <div key={label} className="tick-row__meta-item">
              <span className="tick-row__meta-label">{label}</span>
              <span>{value}</span>
            </div>
          ))}
        </div>
      </div>

      {showDetails && (
        <div
          className="tick-modal-backdrop"
          onClick={() => setShowDetails(false)}
        >
          <div
            aria-labelledby={`tick-details-title-${tick.id}`}
            aria-modal="true"
            className="tick-modal"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
          >
            <div className="tick-modal__header">
              <h2 id={`tick-details-title-${tick.id}`}>{tick.climbName}</h2>
              <button onClick={() => setShowDetails(false)}>Close</button>
            </div>

            {tick.notes && <p>{tick.notes}</p>}
            {tick.sourceUrl && (
              <p>
                Source URL: <a href={tick.sourceUrl}>{tick.sourceUrl}</a>
              </p>
            )}
            {details.map(([label, value]) => (
              <p key={label}>
                {label}: {value}
              </p>
            ))}
          </div>
        </div>
      )}

      {editing && (
        <div className="tick-row__edit-form">
          <TickForm
            initialValues={toFormValues(tick)}
            submitLabel="Save"
            onSubmit={updateTick}
            onCancel={() => {
              setDeleteError("");
              setEditing(false);
            }}
          />
        </div>
      )}
    </div>
  );

  async function deleteTick() {
    if (isDeleting) {
      return;
    }

    setDeleteError("");
    setIsDeleting(true);
    try {
      const response = await fetch(`/ticks/${tick.id}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      try {
        if (response.status === 401) {
          onAuthExpired();
          return;
        }

        if (response.status === 403) {
          setDeleteError("The backend rejected the delete request (403).");
          return;
        }

        if (!response.ok) {
          setDeleteError(`Could not delete tick (${response.status}).`);
          return;
        }
      } catch (error) {
        setDeleteError(`Could not delete tick: ${error}`);
        return;
      }

      await onDelete();
      return;
    } finally {
      setIsDeleting(false);
    }
  }

  async function updateTick(values: TickFormValues) {
    const response = await fetch(`/ticks/${tick.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(toTickPayload(values)),
    });

    if (!response.ok) {
      if (response.status === 401) {
        onAuthExpired();
        throw new Error("Your session expired. Please log in again.");
      }

      if (response.status === 403) {
        throw new Error("The backend rejected the edit request (403).");
      }

      throw new Error(`Could not update tick (${response.status}).`);
    }

    await onUpdate();
    setEditing(false);
  }
}

export default TickCard;

function toFormValues(tick: TickRecord): TickFormValues {
  return {
    climbName: tick.climbName ?? "",
    climbId: tick.climbId ?? "",
    location: tick.location ?? "",
    discipline: tick.discipline ?? "UNKNOWN",
    grade: tick.grade ?? "",
    gradeSystem: tick.gradeSystem ?? "UNKNOWN",
    sourceApp: tick.sourceApp ?? "UNKNOWN",
    tickType: tick.tickType ?? "UNKNOWN",
    externalId: tick.externalId ?? "",
    sourceUrl: tick.sourceUrl ?? "",
    tickDate: tick.tickDate ?? "",
    style: tick.style ?? "",
    ropeStyle: tick.ropeStyle ?? "UNKNOWN",
    attempts: tick.attempts?.toString() ?? "",
    notes: tick.notes ?? "",
  };
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return "Unknown";
  }

  return new Date(`${value}T00:00:00`).toLocaleDateString();
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "Unknown";
  }

  return new Date(value).toLocaleString();
}

type ActionButtonProps = {
  children: ReactNode;
  disabled?: boolean;
  danger?: boolean;
  label: string;
  onClick: () => void;
};

function ActionButton({
  children,
  danger = false,
  disabled = false,
  label,
  onClick,
}: ActionButtonProps) {
  return (
    <button
      className={`icon-button${danger ? " icon-button--danger" : ""}`}
      disabled={disabled}
      onClick={onClick}
      aria-label={label}
      title={label}
      type="button"
    >
      {children}
    </button>
  );
}
