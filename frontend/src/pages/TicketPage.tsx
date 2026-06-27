import { useCallback, useEffect, useState } from "react";
import TicketCard from "../components/TicketCard";
import TicketForm from "../components/TicketForm";
import {
  DEFAULT_TICKET_VALUES,
  FILTER_OPTIONS,
  type TicketFormValues,
} from "../ticketConfig";
import "../App.css";

type Ticket = {
  id: number;
  title: string;
  description: string;
  status: string;
  priority: string;
  assignee: string;
  createdAt: string;
  updatedAt: string;
};

type TicketPageProps = {
  onAuthExpired: () => void;
  token: string;
};

function TicketPage({ onAuthExpired, token }: TicketPageProps) {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [searchFilter, setSearchFilter] = useState("");
  const [message, setMessage] = useState("");

  const loadTickets = useCallback(async () => {
    const response = await fetch("/tickets", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      if (response.status === 401) {
        onAuthExpired();
        setMessage("Your session expired. Please log in again.");
        return;
      }

      if (response.status === 403) {
        setMessage("The backend rejected this request (403).");
        return;
      }

      setMessage(`Could not load tickets (${response.status}).`);
      return;
    }

    const data = await response.json();
    setTickets(data);
    setMessage("");
  }, [token]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  async function createTicket(values: TicketFormValues) {
    const response = await fetch("/ticket", {
      method: "POST",
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
        throw new Error("The backend rejected the create request (403).");
      }

      throw new Error(`Could not create ticket (${response.status}).`);
    }

    await loadTickets();
  }

  const filteredTickets = tickets.filter((ticket) => {
    const matchesStatus =
      statusFilter === "ALL" || ticket.status === statusFilter;
    const matchesPriority =
      priorityFilter === "ALL" || ticket.priority === priorityFilter;
    const matchesSearch =
      searchFilter === "" ||
      ticket.description.toLowerCase().includes(searchFilter) ||
      ticket.title.toLowerCase().includes(searchFilter);

    return matchesStatus && matchesPriority && matchesSearch;
  });
  return (
    <main className = "ticket-page">
      <TicketForm
        className="create-ticket-form"
        heading="Create New Ticket"
        initialValues={DEFAULT_TICKET_VALUES}
        resetOnSubmit
        submitLabel="Create Ticket"
        onSubmit={createTicket}
      />
      <div className="filters">
        <input
          type="text"
          placeholder="Search"
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          {FILTER_OPTIONS.status.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
        >
          {FILTER_OPTIONS.priority.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      {message && <p role="alert">{message}</p>}
      {filteredTickets.map((ticket) => (
        <TicketCard
          key={ticket.id}
          id={ticket.id}
          title={ticket.title}
          description={ticket.description}
          status={ticket.status}
          priority={ticket.priority}
          assignee={ticket.assignee}
          createdAt={ticket.createdAt}
          updatedAt={ticket.updatedAt}
          onAuthExpired={onAuthExpired}
          onDelete={loadTickets}
          onUpdate={loadTickets}
          token={token}
        />
      ))}
      
    </main>
  );
}

export default TicketPage;
