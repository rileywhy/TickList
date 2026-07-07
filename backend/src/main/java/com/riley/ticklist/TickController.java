package com.riley.ticklist;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TickController {

    private final TickRepository tickRepository;

    public TickController(TickRepository tickRepository) {
        this.tickRepository = tickRepository;
    }

    @GetMapping({"/ticks", "/tick"})
    public List<Tick> getTicks(@AuthenticationPrincipal User user) {
        return tickRepository.findByUser(user);


    }

    @GetMapping({"/ticks/{id}", "/tick/{id}"})
    public Tick getTick(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        return tickRepository.findByIdAndUser(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tick not found."));

    }

    @PostMapping({"/ticks", "/tick"})
    public Tick createTick(@RequestBody Tick tick, @AuthenticationPrincipal User user) {
        tick.setGradeValue(resolveGradeValue(tick));
        tick.setUser(user);
        return tickRepository.save(tick);
    }

    @PutMapping({"/ticks/{id}", "/tick/{id}"})
    public Tick updateTick(
        @PathVariable("id") Long id,
        @RequestBody Tick updatedTick,
        @AuthenticationPrincipal User user
    ) {
        Tick existingTick = tickRepository.findByIdAndUser(id, user).orElse(null);

         if (existingTick == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tick not found.");
        }

        existingTick.setClimbName(updatedTick.getClimbName());
        existingTick.setClimbId(updatedTick.getClimbId());
        existingTick.setLocation(updatedTick.getLocation());
        existingTick.setDiscipline(updatedTick.getDiscipline());
        existingTick.setTickType(updatedTick.getTickType());
        existingTick.setGradeValue(resolveGradeValue(updatedTick));
        existingTick.setGrade(updatedTick.getGrade());
        existingTick.setRawGrade(updatedTick.getRawGrade());
        existingTick.setGradeSystem(updatedTick.getGradeSystem());
        existingTick.setGradeMapping(updatedTick.getGradeMapping());
        existingTick.setSourceApp(updatedTick.getSourceApp());
        existingTick.setExternalId(updatedTick.getExternalId());
        existingTick.setSourceUrl(updatedTick.getSourceUrl());
        existingTick.setStyle(updatedTick.getStyle());
        existingTick.setRopeStyle(updatedTick.getRopeStyle());
        existingTick.setTickDate(updatedTick.getTickDate());
        existingTick.setAttempts(updatedTick.getAttempts());
        existingTick.setNotes(updatedTick.getNotes());
        existingTick.setClimbHeight(updatedTick.getClimbHeight());
        return tickRepository.save(existingTick);
    }

    @DeleteMapping({"/ticks/{id}", "/tick/{id}"})
    public void deleteTick(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        Tick tick = tickRepository.findByIdAndUser(id, user).orElse(null);
        if (tick == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tick not found.");
        }
        tickRepository.delete(tick);

    }

    // gradeValue is derived from the grade string unless the client supplies one,
    // so ticks created or edited through the UI stay sortable by grade.
    private static Double resolveGradeValue(Tick tick) {
        if (tick.getGradeValue() != null) {
            return tick.getGradeValue();
        }

        return GradeParser.parseGradeValue(tick.getGrade());
    }  
}
