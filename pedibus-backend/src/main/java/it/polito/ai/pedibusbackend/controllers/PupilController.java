package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.PupilService;
import it.polito.ai.pedibusbackend.viewmodels.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PupilController {
    @Autowired
    private PupilService pupilService;


    @PostMapping(value = "/pupils", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> createPupil(@RequestBody @Valid NewPupilDTO newPupil, Principal principal)
            throws BadRequestException, ForbiddenException {
        Long id = pupilService.createPupil(newPupil, principal.getName());

        Map<String, Long> response = new HashMap<>();
        response.put("id", id);

        return response;
    }

    @PutMapping(value = "/pupils/{pupilId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updatePupil(@PathVariable Long pupilId, @RequestBody PupilUpdateDTO pupilUpdate, Principal principal)
            throws BadRequestException, NotFoundException, ForbiddenException {
        pupilService.updatePupil(pupilId, pupilUpdate, principal.getName());
    }

    @DeleteMapping(value = "/pupils/{pupilId}")
    public void deletePupil(@PathVariable Long pupilId, Principal principal)
            throws BadRequestException, NotFoundException, ForbiddenException {
        pupilService.deletePupil(pupilId, principal.getName());
    }

    @GetMapping(value = "/pupils/{pupilId}/reservations", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReservationDTO> getReservations(@PathVariable Long pupilId, Principal principal)
            throws BadRequestException, NotFoundException, ForbiddenException {
        return pupilService.getReservations(pupilId, principal.getName());
    }
}
