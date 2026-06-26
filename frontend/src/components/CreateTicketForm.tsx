import { useState } from "react";



type CreateTicketFormProps = {
    loadTickets: () => void;
    onDelete:() => void;
}


function CreateTicketForm({ loadTickets
    
}: CreateTicketFormProps) {

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [status, setStatus] = useState("");
    const [priority, setPriority] = useState("");
    const [assignee, setAssignee] = useState("");


    function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

  fetch("http://localhost:8080/ticket", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      title,
      description,
      status,
      priority,
      assignee,
    }),
  } ).then(() => {
    loadTickets();
  });
  
}


  return (
    <form className="create-ticket-form" onSubmit={handleSubmit}>
        <h2>Create New Ticket</h2>
        <input
            type="text"
            placeholder="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
        />
        <textarea
            placeholder="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
        ></textarea>
        <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
        >
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="IN_REVIEW">In Review</option>
            <option value="DONE">Done</option>
        </select>
        <select
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
        >
            <option value="">Select Priority</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
        </select>
        <input
            type="text"
            placeholder="Assignee"
            value={assignee}
            onChange={(e) => setAssignee(e.target.value)}
        />
        <button type="submit">Create Ticket</button>
    </form>

  );
    
}



export default CreateTicketForm;