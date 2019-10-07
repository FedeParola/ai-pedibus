package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.ReservationService;
import it.polito.ai.pedibusbackend.viewmodels.ReservationDTO;
import it.polito.ai.pedibusbackend.viewmodels.ReservationsDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Date;

@RestController
public class ReservationController {
//    @Autowired
//    private ReservationService reservationService;
//
//    @RequestMapping(value = "/reservations/{lineName}/{date}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    public ReservationsDTO getReservations(@PathVariable String lineName,
//                                           @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) throws NotFoundException, BadRequestException, ForbiddenException {
//        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//
//        return reservationService.getReservations(lineName, date, loggedUser);
//    }
//
//    @RequestMapping(value = "/reservations/{lineName}/{date}", method = RequestMethod.POST,
//                    produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
//    public Long createReservation(@PathVariable String lineName, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
//                                  @RequestBody @Valid ReservationDTO reservation, BindingResult bindingResult,
//                                  HttpServletResponse response) throws BadRequestException, NotFoundException {
//        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//
//        if(bindingResult.hasErrors()) {
//            StringBuilder errMsg = new StringBuilder("Invalid format of the request body:");
//            for (FieldError err : bindingResult.getFieldErrors()) {
//                errMsg.append(" " + err.getField() + ": " + err.getDefaultMessage() + ";");
//            }
//            throw new BadRequestException(errMsg.toString());
//        }
//
//        Long reservationId = reservationService.addReservation(reservation, lineName, date, loggedUser);
//        response.setStatus(HttpServletResponse.SC_CREATED);
//
//        return reservationId;
//    }
//
//    @RequestMapping(value = "/reservations/{lineName}/{date}/{reservationId}", method = RequestMethod.PUT)
//    public void updateReservation(@PathVariable String lineName, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date, @PathVariable Long reservationId,
//                                  @RequestBody ReservationDTO reservation) throws NotFoundException, BadRequestException, ForbiddenException {
//        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        reservationService.updateReservation(lineName, date, reservationId, reservation, loggedUser);
//
//        return;
//    }
//
//    @RequestMapping(value = "/reservations/{lineName}/{date}/{reservationId}", method = RequestMethod.DELETE)
//    public void deleteReservation(@PathVariable String lineName, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
//                                  @PathVariable Long reservationId) throws NotFoundException, BadRequestException, ForbiddenException {
//        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        reservationService.deleteReservation(lineName, date, reservationId, loggedUser);
//
//        return;
//    }
//
//    @RequestMapping(value = "/reservations/{lineName}/{date}/{reservationId}", method = RequestMethod.GET,
//                    produces = MediaType.APPLICATION_JSON_VALUE)
//    public ReservationDTO getReservation(@PathVariable String lineName,
//                                         @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
//                                         @PathVariable Long reservationId) throws NotFoundException, ForbiddenException, BadRequestException {
//        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        return reservationService.getReservation(lineName, date, reservationId, loggedUser);
//    }
}
