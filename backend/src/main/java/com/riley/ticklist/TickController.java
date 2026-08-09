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

import jakarta.validation.Valid;

@RestController
public class TickController {

    private final TickRepository tickRepository;
    private final GradeMappingService gradeMappingService;

    public TickController(TickRepository tickRepository, GradeMappingService gradeMappingService) {
        this.tickRepository = tickRepository;
        this.gradeMappingService = gradeMappingService;
    }

    @GetMapping({"/ticks", "/tick"})
    public List<TickResponse> getTicks(@AuthenticationPrincipal User user) {
        return tickRepository.findByUser(user).stream()
            .map(TickResponse::from)
            .toList();
    }

    @GetMapping({"/ticks/{id}", "/tick/{id}"})
    public TickResponse getTick(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        return tickRepository.findByIdAndUser(id, user)
            .map(TickResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tick not found."));
    }

    @PostMapping({"/ticks", "/tick"})
    public TickResponse createTick(@Valid @RequestBody TickRequest request, @AuthenticationPrincipal User user) {
        Tick tick = new Tick();
        applyRequest(request, tick);
        gradeMappingService.applyGradeMapping(tick);
        tick.setUser(user);
        return TickResponse.from(tickRepository.save(tick));
    }

    @PutMapping({"/ticks/{id}", "/tick/{id}"})
    public TickResponse updateTick(
        @PathVariable("id") Long id,
        @Valid @RequestBody TickRequest request,
        @AuthenticationPrincipal User user
    ) {
        Tick existingTick = tickRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tick not found."));

        applyRequest(request, existingTick);
        gradeMappingService.applyGradeMapping(existingTick);
        return TickResponse.from(tickRepository.save(existingTick));
    }

    @DeleteMapping({"/ticks/{id}", "/tick/{id}"})
    public void deleteTick(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        Tick tick = tickRepository.findByIdAndUser(id, user).orElse(null);
        if (tick == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tick not found.");
        }
        tickRepository.delete(tick);
    }

    /**
     * Translate at the boundary: only TickRequest's fields cross onto the
     * entity; id and user are set nowhere here, so a request cannot touch them.
     * Enums fall back to UNKNOWN when omitted, matching the entity's defaults.
     */
    private void applyRequest(TickRequest request, Tick tick) {
        tick.setClimbName(request.climbName());
        tick.setClimbId(request.climbId());
        tick.setLocation(request.location());
        tick.setDiscipline(request.discipline() != null ? request.discipline() : Discipline.UNKNOWN);
        tick.setTickType(request.tickType() != null ? request.tickType() : TickType.UNKNOWN);
        tick.setGrade(request.grade());
        tick.setGradeSystem(request.gradeSystem() != null ? request.gradeSystem() : GradeSystem.UNKNOWN);
        tick.setSourceApp(request.sourceApp() != null ? request.sourceApp() : SourceApp.UNKNOWN);
        tick.setExternalId(request.externalId());
        tick.setSourceUrl(request.sourceUrl());
        tick.setStyle(request.style());
        tick.setRopeStyle(request.ropeStyle() != null ? request.ropeStyle() : RopeStyle.UNKNOWN);
        tick.setTickDate(request.tickDate());
        tick.setAttempts(request.attempts());
        tick.setNotes(request.notes());
    }
}
