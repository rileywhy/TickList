package com.riley.ticklist;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SendController {

    private final SendRepository sendRepository;

    public SendController(SendRepository sendRepository) {
        this.sendRepository = sendRepository;
    }

    @GetMapping({"/sends", "/send"})
    public List<Send> getSends() {
        return sendRepository.findAll();
    }

    @GetMapping({"/sends/{id}", "/send/{id}"})
    public Send getSend(@PathVariable("id") Long id) {
        return sendRepository.findById(id).orElse(null);
    }

    @PostMapping({"/sends", "/send"})
    public Send createSend(@RequestBody Send send) {
        return sendRepository.save(send);
    }

    @PutMapping({"/sends/{id}", "/send/{id}"})
    public Send updateSend(
        @PathVariable("id") Long id,
        @RequestBody Send updatedSend
    ) {
        Send existingSend = sendRepository.findById(id).orElse(null);

        if (existingSend == null) {
            return null;
        }

        existingSend.setClimbName(updatedSend.getClimbName());
        existingSend.setClimbId(updatedSend.getClimbId());
        existingSend.setUser(updatedSend.getUser());
        existingSend.setLocation(updatedSend.getLocation());
        existingSend.setDiscipline(updatedSend.getDiscipline());
        existingSend.setGradeValue(updatedSend.getGradeValue());
        existingSend.setGrade(updatedSend.getGrade());
        existingSend.setRawGrade(updatedSend.getRawGrade());
        existingSend.setGradeSystem(updatedSend.getGradeSystem());
        existingSend.setGradeMapping(updatedSend.getGradeMapping());
        existingSend.setSourceApp(updatedSend.getSourceApp());
        existingSend.setExternalId(updatedSend.getExternalId());
        existingSend.setSourceUrl(updatedSend.getSourceUrl());
        existingSend.setStyle(updatedSend.getStyle());
        existingSend.setRopeSendStyle(updatedSend.getRopeSendStyle());
        existingSend.setSendDate(updatedSend.getSendDate());
        existingSend.setAttempts(updatedSend.getAttempts());
        existingSend.setNotes(updatedSend.getNotes());
        existingSend.setClimbHeight(updatedSend.getClimbHeight());
        return sendRepository.save(existingSend);
    }

    @DeleteMapping({"/sends/{id}", "/send/{id}"})
    public void deleteSend(@PathVariable("id") Long id) {
        sendRepository.deleteById(id);
    }
}
