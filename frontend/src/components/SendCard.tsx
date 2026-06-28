import { useState, type ReactNode } from "react";
import SendForm from "./SendForm";
import {
  formatEnumLabel,
  getSourceMeta,
  toSendPayload,
  type SendFormValues,
  type SendRecord,
} from "../sendConfig";

type SendCardProps = {
  send: SendRecord;
  onAuthExpired: () => void;
  onDelete: () => Promise<void>;
  onUpdate: () => Promise<void>;
  token: string;
};

function SendCard({
  send,
  onAuthExpired,
  onDelete,
  onUpdate,
  token,
}: SendCardProps) {
  const [showDetails, setShowDetails] = useState(false);
  const [editing, setEditing] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const sourceMeta = getSourceMeta(send.sourceApp);
  const gradeLabel = send.grade
    ? `${send.grade} (${formatEnumLabel(send.gradeSystem)})`
    : formatEnumLabel(send.gradeSystem);
  const details = [
    ["Area", send.areaName || "Unknown"],
    ["Grade", gradeLabel],
    ["Source", sourceMeta.label],
    ["Send style", formatEnumLabel(send.sendStyle)],
    ["Send date", formatDate(send.sendDate)],
    ["Attempts", send.attempts?.toString() ?? "Unknown"],
    ["External ID", send.externalId || "None"],
    ["Climb ID", send.climbId || "None"],
    ["Created", formatDateTime(send.createdAt)],
    ["Updated", formatDateTime(send.updatedAt)],
  ];

  return (
    <div className="send-card send-row">
      <div className="send-row__main">
        <div className="send-row__header">
          <div className="send-row__title-group">
            <h3 className="send-row__title">{send.climbName}</h3>
            <span className={`send-source send-source--${sourceMeta.tone}`}>
              <span className="send-source__icon" aria-hidden="true">
                {sourceMeta.icon}
              </span>
              <span>{sourceMeta.label}</span>
            </span>
          </div>

          <div className="send-row__actions">
            <ActionButton label="Show Details" onClick={() => setShowDetails(true)}>
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M12 5c5.23 0 9.27 4.1 10.7 6.02a1.6 1.6 0 0 1 0 1.96C21.27 14.9 17.23 19 12 19S2.73 14.9 1.3 12.98a1.6 1.6 0 0 1 0-1.96C2.73 9.1 6.77 5 12 5Zm0 2C8.3 7 5.23 9.73 3.42 12 5.23 14.27 8.3 17 12 17s6.77-2.73 8.58-5C18.77 9.73 15.7 7 12 7Zm0 2.25A2.75 2.75 0 1 1 9.25 12 2.75 2.75 0 0 1 12 9.25Z" />
              </svg>
            </ActionButton>

            {!editing && (
              <ActionButton
                label="Edit send"
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
              label={isDeleting ? "Deleting send" : "Delete send"}
              onClick={deleteSend}
            >
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M6.7 5.3 12 10.59l5.3-5.3 1.4 1.41L13.41 12l5.3 5.29-1.41 1.41L12 13.41l-5.29 5.3-1.41-1.42L10.59 12 5.3 6.71 6.7 5.3Z" />
              </svg>
            </ActionButton>
          </div>
        </div>

        {send.notes && <p className="send-row__description">{send.notes}</p>}
        {deleteError && <p role="alert">{deleteError}</p>}

        <div className="send-row__meta">
          {details.slice(0, 6).map(([label, value]) => (
            <div key={label} className="send-row__meta-item">
              <span className="send-row__meta-label">{label}</span>
              <span>{value}</span>
            </div>
          ))}
        </div>
      </div>

      {showDetails && (
        <div className="send-modal-backdrop" onClick={() => setShowDetails(false)}>
          <div
            aria-labelledby={`send-details-title-${send.id}`}
            aria-modal="true"
            className="send-modal"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
          >
            <div className="send-modal__header">
              <h2 id={`send-details-title-${send.id}`}>{send.climbName}</h2>
              <button onClick={() => setShowDetails(false)}>Close</button>
            </div>

            {send.notes && <p>{send.notes}</p>}
            {send.sourceUrl && (
              <p>
                Source URL: <a href={send.sourceUrl}>{send.sourceUrl}</a>
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
        <div className="send-row__edit-form">
          <SendForm
            initialValues={toFormValues(send)}
            submitLabel="Save"
            onSubmit={updateSend}
            onCancel={() => {
              setDeleteError("");
              setEditing(false);
            }}
          />
        </div>
      )}
    </div>
  );

  async function deleteSend() {
    if (isDeleting) {
      return;
    }

    setDeleteError("");
    setIsDeleting(true);

    const response = await fetch(`/sends/${send.id}`, {
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
        setDeleteError(`Could not delete send (${response.status}).`);
        return;
      }

      await onDelete();
    } finally {
      setIsDeleting(false);
    }
  }

  async function updateSend(values: SendFormValues) {
    const response = await fetch(`/sends/${send.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(toSendPayload(values)),
    });

    if (!response.ok) {
      if (response.status === 401) {
        onAuthExpired();
        throw new Error("Your session expired. Please log in again.");
      }

      if (response.status === 403) {
        throw new Error("The backend rejected the edit request (403).");
      }

      throw new Error(`Could not update send (${response.status}).`);
    }

    await onUpdate();
    setEditing(false);
  }
}

export default SendCard;

function toFormValues(send: SendRecord): SendFormValues {
  return {
    climbName: send.climbName ?? "",
    climbId: send.climbId ?? "",
    areaName: send.areaName ?? "",
    grade: send.grade ?? "",
    gradeSystem: send.gradeSystem ?? "UNKNOWN",
    sourceApp: send.sourceApp ?? "UNKNOWN",
    externalId: send.externalId ?? "",
    sourceUrl: send.sourceUrl ?? "",
    sendDate: send.sendDate ?? "",
    sendStyle: send.sendStyle ?? "UNKNOWN",
    attempts: send.attempts?.toString() ?? "",
    notes: send.notes ?? "",
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
