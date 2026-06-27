import { useState } from "react";
import EditTicketForm from "./EditTicketForm";


type TicketCardProps = {
  title: string;
  description: string;
  status: string;
  priority: string;
  assignee: string;
  createdAt: string;
  updatedAt: string;
  id: number;
  onDelete: () => void;
  onUpdate: () => void;
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
  onDelete,
  onUpdate,
  token


}: TicketCardProps) {

  const [expanded, setExpanded] = useState(false);
  const [editing, setEditing] = useState(false);

  return (
    <div className="ticket-card">
  <h2>{title}</h2>

  <button className="delete-button" onClick={deleteTicket}>
    Delete Ticket
  </button>

  <button onClick={() => setExpanded(!expanded)}>
    {expanded ? "Hide Details" : "Show Details"}
  </button>

  {expanded && <p>{description}</p>}

  <p>{status} - {priority}</p>

  <p>Assigned to: {assignee}</p>

  <p>Created at: {new Date(createdAt).toLocaleString()}</p>

  <p>Last updated: {new Date(updatedAt).toLocaleString()}</p>

  {editing ? (<EditTicketForm
    ticket={{
      id,
      title,
      description,
      status,
      priority,
      assignee,
    }}
    onUpdate={onUpdate}
    onCancel={() => setEditing(false)}
    token = {token}
  />

  ) : (
    <button onClick={() => setEditing(true)}>Edit Ticket</button>
  )}
</div>
  );
  

  function deleteTicket() {
    fetch(`/ticket/${id}`, {
      method: "DELETE", 
    headers: {
      Authorization: `Bearer ${token}`,
    },
    }).then(() => {
      onDelete();
    });
  }
}

export default TicketCard;
