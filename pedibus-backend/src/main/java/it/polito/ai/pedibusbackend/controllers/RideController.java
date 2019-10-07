package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.entities.Attendance;
import it.polito.ai.pedibusbackend.entities.Availability;
import it.polito.ai.pedibusbackend.entities.Reservation;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.RideService;
import it.polito.ai.pedibusbackend.viewmodels.NewRideDTO;
import it.polito.ai.pedibusbackend.viewmodels.RideDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@RestController
public class RideController {
    @Autowired
    private RideService rideService;

    @RequestMapping(value = "/rides", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RideDTO> getRides() {
        return rideService.getRides();
    }

    @RequestMapping(value = "/rides", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Long createRide(@RequestBody @Valid NewRideDTO ride, BindingResult bindingResult,
                           HttpServletResponse response) throws BadRequestException, NotFoundException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(bindingResult.hasErrors()) {
            StringBuilder errMsg = new StringBuilder("Invalid format of the request body:");
            for (FieldError err : bindingResult.getFieldErrors()) {
                errMsg.append(" " + err.getField() + ": " + err.getDefaultMessage() + ";");
            }
            throw new BadRequestException(errMsg.toString());
        }

        Long rideId = rideService.addRide(ride, loggedUser);
        response.setStatus(HttpServletResponse.SC_CREATED);

        return rideId;
    }

    @RequestMapping(value = "/rides/{rideId}", method = RequestMethod.DELETE)
    public void deleteRide(@PathVariable Long rideId) throws NotFoundException, BadRequestException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        rideService.deleteRide(rideId, loggedUser);
        return;
    }

    @RequestMapping(value = "/rides/{rideId}/reservations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Reservation> getRideReservations(@PathVariable Long rideId) throws NotFoundException, BadRequestException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return rideService.getRideReservations(rideId, loggedUser);
    }

    @RequestMapping(value = "/rides/{rideId}/attendances", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Attendance> getRideAttendances(@PathVariable Long rideId) throws NotFoundException, BadRequestException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return rideService.getRideAttendances(rideId, loggedUser);
    }

    @RequestMapping(value = "/rides/{rideId}/availabilities", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Availability> getRideAvailabilities(@PathVariable Long rideId) throws NotFoundException, BadRequestException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return rideService.getRideAvailabilities(rideId, loggedUser);
    }

}
