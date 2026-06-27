import { useState, type ReactNode } from "react";
import TicketForm from "./TicketForm";
import {
  formatEnumLabel,
  getStatusMeta,
  type TicketFormValues,
} from "../ticketConfig";

type TicketCardProps = {
  title: string;
  description: string;
  status: string;
  priority: string;
  assignee: string;
  createdAt: string;
  updatedAt: string;
  id: number;
  onAuthExpired: () => void;
  onDelete: () => Promise<void>;
  onUpdate: () => Promise<void>;
  token: string;
};

function TicketCard({
  title,
  description,
  status,
  priority,
  assignee,
  createdAt,
  updatedAt,
  id,
  onAuthExpired,
  onDelete,
  onUpdate,
  token
}: TicketCardProps) {
  const [showDetails, setShowDetails] = useState(false);
  const [editing, setEditing] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const statusMeta = getStatusMeta(status);
  const details = [
    ["Priority", formatEnumLabel(priority)],
    ["Assignee", assignee || "Unassigned"],
    ["Created", new Date(createdAt).toLocaleString()],
    ["Updated", new Date(updatedAt).toLocaleString()],
  ];

  return (
    <div className="ticket-card ticket-row">
      <div className="ticket-row__main">
        <div className="ticket-row__header">
          <div className="ticket-row__title-group">
            <h3 className="ticket-row__title">{title}</h3>
            <span className={`ticket-status ticket-status--${statusMeta.tone}`}>
              <span className="ticket-status__icon" aria-hidden="true">
                {statusMeta.icon}
              </span>
              <span>{statusMeta.label}</span>
            </span>
          </div>

          <div className="ticket-row__actions">
            <ActionButton label="Show Details" onClick={() => setShowDetails(true)}>
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M12 5c5.23 0 9.27 4.1 10.7 6.02a1.6 1.6 0 0 1 0 1.96C21.27 14.9 17.23 19 12 19S2.73 14.9 1.3 12.98a1.6 1.6 0 0 1 0-1.96C2.73 9.1 6.77 5 12 5Zm0 2C8.3 7 5.23 9.73 3.42 12 5.23 14.27 8.3 17 12 17s6.77-2.73 8.58-5C18.77 9.73 15.7 7 12 7Zm0 2.25A2.75 2.75 0 1 1 9.25 12 2.75 2.75 0 0 1 12 9.25Z" />
              </svg>
            </ActionButton>

            {!editing && (
              <ActionButton
                label="Edit Ticket"
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
              label={isDeleting ? "Deleting Ticket" : "Delete Ticket"}
              onClick={deleteTicket}
            >
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M6.7 5.3 12 10.59l5.3-5.3 1.4 1.41L13.41 12l5.3 5.29-1.41 1.41L12 13.41l-5.29 5.3-1.41-1.42L10.59 12 5.3 6.71 6.7 5.3Z" />
              </svg>
            </ActionButton>
          </div>
        </div>

        <p className="ticket-row__description">{description}</p>
        {deleteError && <p role="alert">{deleteError}</p>}

        <div className="ticket-row__meta">
          {details.map(([label, value]) => (
            <div key={label} className="ticket-row__meta-item">
              <span className="ticket-row__meta-label">{label}</span>
              <span>{value}</span>
            </div>
          ))}
        </div>
      </div>

      {showDetails && (
        <div className="ticket-modal-backdrop" onClick={() => setShowDetails(false)}>
          <div
            aria-labelledby={`ticket-details-title-${id}`}
            aria-modal="true"
            className="ticket-modal"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
          >
            <div className="ticket-modal__header">
              <h2 id={`ticket-details-title-${id}`}>{title}</h2>
              <button onClick={() => setShowDetails(false)}>Close</button>
            </div>

            <p>{description}</p>
            <p>Status: {statusMeta.label}</p>
            {details.map(([label, value]) => (
              <p key={label}>
                {label}: {value}
              </p>
            ))}
          </div>
        </div>
      )}

      {editing && (
        <div className="ticket-row__edit-form">
          <TicketForm
            initialValues={{ title, description, status, priority, assignee }}
            submitLabel="Save"
            onSubmit={updateTicket}
            onCancel={() => {
              setDeleteError("");
              setEditing(false);
            }}
          />
        </div>
      )}
    </div>
  );

  async function deleteTicket() {
    if (isDeleting) {
      return;
    }

    setDeleteError("");
    setIsDeleting(true);

    const response = await fetch(`/ticket/${id}`, {
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
        setDeleteError(`Could not delete ticket (${response.status}).`);
        return;
      }

      await onDelete();
    } finally {
      setIsDeleting(false);
    }
  }

  async function updateTicket(values: TicketFormValues) {
    const response = await fetch(`/ticket/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(values),
    });

    if (!response.ok) {
      if (response.status === 401) {
        onAuthExpired();
        throw new Error("Your session expired. Please log in again.");
      }

      if (response.status === 403) {
        throw new Error("The backend rejected the edit request (403).");
      }

      throw new Error(`Could not update ticket (${response.status}).`);
    }

    await onUpdate();
    setEditing(false);
  }
}

export default TicketCard;

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
