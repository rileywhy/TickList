import { useEffect, useState } from "react";
import TicketCard from "../components/TicketCard";
import CreateTicketForm from "../components/CreateTicketForm";
import "../App.css"

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

function TicketPage() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [searchFilter, setSearchFilter] = useState("");


  function loadTickets() {
    fetch("http://localhost:8080/tickets")
      .then((response) => response.json())
      .then((data) => setTickets(data));
  }
  useEffect(() => {
    loadTickets();
  }, []);

  const filteredTickets = tickets.filter((ticket) => {
    const matchesStatus = 
      statusFilter === "ALL" || ticket.status === statusFilter;
    const matchesPriority =
    priorityFilter === "ALL" || ticket.priority === priorityFilter;
    const matchesSearch = 
      searchFilter === "" || ticket.description.toLowerCase().includes(searchFilter) || ticket.title.toLowerCase().includes(searchFilter); 

  return matchesStatus && matchesPriority && matchesSearch;



  });
  return (
    <main>
      <h1>Issue Tracker</h1>
      <div className="filters">
      <input
            type="text"
            placeholder="Search"
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
        /> 
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="ALL">All Statuses</option>
          <option value="OPEN">Open</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="CLOSED">Closed</option>
        </select>
        <select value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)}>
          <option value="ALL">All Priorities</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
          <option value="CRITICAL">Critical</option>
        </select>
      </div>
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
          onDelete={loadTickets}
          onUpdate={loadTickets}
        />
      ))}
      <CreateTicketForm loadTickets={loadTickets} onDelete={loadTickets} />
    </main>
  );
}


export default TicketPage;