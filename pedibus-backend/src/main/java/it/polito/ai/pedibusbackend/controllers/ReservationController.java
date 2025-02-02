package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.ReservationService;
import it.polito.ai.pedibusbackend.viewmodels.NewReservationDTO;
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
public class ReservationController {
    @Autowired
    private ReservationService reservationService;

    @RequestMapping(value="/reservations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Long addReservation(@RequestBody @Valid NewReservationDTO reservation, BindingResult bindingResult,
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

        Long reservationId = reservationService.addReservation(reservation, loggedUser);
        response.setStatus(HttpServletResponse.SC_CREATED);

        return reservationId;
    }

    @RequestMapping(value="/reservations/{reservationId}", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateReservation(@PathVariable Long reservationId, @RequestBody Long newStopId) throws BadRequestException,
            NotFoundException, ForbiddenException {
        //Get logged user
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        reservationService.updateReservation(reservationId, newStopId, loggedUser);
    }

    @RequestMapping(value="/reservations/{reservationId}", method = RequestMethod.DELETE)
    public void deleteReservation(@PathVariable Long reservationId) throws BadRequestException, NotFoundException,
            ForbiddenException {
        //Get logged user
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        reservationService.deleteReservation(reservationId, loggedUser);
    }
}
