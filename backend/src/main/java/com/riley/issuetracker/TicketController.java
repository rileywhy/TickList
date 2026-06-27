package com.riley.issuetracker;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class TicketController {

    private final TicketRepository ticketRepository;

    public TicketController(TicketRepository ticketRepository) {
    this.ticketRepository = ticketRepository;
    }

    @GetMapping("/ticket")
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @GetMapping("/ticket/{id}")
    public Ticket getTicket(@PathVariable("id") Long id) {
        return ticketRepository.findById(id).orElse(null);
    }

    
    @GetMapping("/tickets")
    public List<Ticket> getTickets(){
        return ticketRepository.findAll();
    }

    @PostMapping("/ticket")
    public Ticket createTicket(@RequestBody Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @PutMapping("/ticket/{id}")
    public Ticket updateTicket(
        @PathVariable("id") Long id,
        @RequestBody Ticket updatedTicket
    ) {
        Ticket existingTicket = ticketRepository.findById(id).orElse(null);

        if (existingTicket == null) {
            return null;
    }

    existingTicket.setTitle(updatedTicket.getTitle());
    existingTicket.setDescription(updatedTicket.getDescription());
    existingTicket.setStatus(updatedTicket.getStatus());
    existingTicket.setPriority(updatedTicket.getPriority());
    existingTicket.setAssignee(updatedTicket.getAssignee());

    return ticketRepository.save(existingTicket);
    }

    @DeleteMapping("/ticket/{id}")
    public void deleteTicket(@PathVariable("id") Long id) {
        ticketRepository.deleteById(id);
    }
    
    

}
