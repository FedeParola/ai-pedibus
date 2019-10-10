package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.AvailabilityService;
import it.polito.ai.pedibusbackend.viewmodels.AvailabilityUpdateDTO;
import it.polito.ai.pedibusbackend.viewmodels.NewAvailabilityDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
public class AvailabilityController {
    @Autowired
    private AvailabilityService availabilityService;

    @RequestMapping(value="/availabilities", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Long addAvailability(@RequestBody @Valid NewAvailabilityDTO availability, BindingResult bindingResult,
                                HttpServletResponse response) throws BadRequestException, ForbiddenException {
        //Get logged user
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        //Check validity of the body of the POST
        if(bindingResult.hasErrors()) {
            StringBuilder errMsg = new StringBuilder("Invalid format of the request body:");
            for (FieldError err : bindingResult.getFieldErrors()) {
                errMsg.append(" " + err.getField() + ": " + err.getDefaultMessage() + ";");
            }
            throw new BadRequestException(errMsg.toString());
        }

        Long availabilityId = availabilityService.addAvailability(availability, loggedUser);
        response.setStatus(HttpServletResponse.SC_CREATED);

        return availabilityId;
    }

    @RequestMapping(value="/availabilities/{availabilityId}", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateAvailability(@PathVariable Long availabilityId, @RequestBody AvailabilityUpdateDTO newAvailability)
            throws NotFoundException, BadRequestException, ForbiddenException {
        //Get logged user
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        availabilityService.updateAvailability(availabilityId, newAvailability, loggedUser);
    }

    @RequestMapping(value="/availabilities/{availabilityId}", method = RequestMethod.DELETE)
    public void deleteAvailability(@PathVariable Long availabilityId) throws BadRequestException, NotFoundException,
            ForbiddenException {
        //Get logged user
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        availabilityService.deleteAvailability(availabilityId, loggedUser);
    }
}
