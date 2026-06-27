import { useState } from "react";

type EditTicketFormProps = {
  ticket: {
    id: number;
    title: string;
    description: string;
    status: string;
    priority: string;
    assignee: string;
  };
  onUpdate: () => void;
  onCancel: () => void;
  token: string;

};




function EditTicketForm({ ticket, onUpdate, onCancel, token }: EditTicketFormProps) {
  const [editedTitle, setEditedTitle] = useState(ticket.title);
  const [editedDescription, setEditedDescription] = useState(ticket.description);
  const [editedStatus, setEditedStatus] = useState(ticket.status);
  const [editedPriority, setEditedPriority] = useState(ticket.priority);
  const [editedAssignee, setEditedAssignee] = useState(ticket.assignee);

  function updateTicket() {
    fetch(`/ticket/${ticket.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        title: editedTitle,
        description: editedDescription,
        status: editedStatus,
        priority: editedPriority,
        assignee: editedAssignee,
      }),
    }).then(() => {
      onUpdate();
      onCancel();
    });
  }

  return (
    <>
      <input value={editedTitle} onChange={(e) => setEditedTitle(e.target.value)} />

      <textarea
        value={editedDescription}
        onChange={(e) => setEditedDescription(e.target.value)}
      />

      <input value={editedStatus} onChange={(e) => setEditedStatus(e.target.value)} />

      <input value={editedPriority} onChange={(e) => setEditedPriority(e.target.value)} />

      <input value={editedAssignee} onChange={(e) => setEditedAssignee(e.target.value)} />

      <button onClick={updateTicket}>Save</button>
      <button onClick={onCancel}>Cancel</button>
    </>
  );
}

export default EditTicketForm;
