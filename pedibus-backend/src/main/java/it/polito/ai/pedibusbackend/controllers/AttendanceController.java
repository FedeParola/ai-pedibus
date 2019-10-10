package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.AttendanceService;
import it.polito.ai.pedibusbackend.viewmodels.NewAttendanceDTO;
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
public class AttendanceController {
    @Autowired
    private AttendanceService attendanceService;

    @RequestMapping(value="/attendances", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Long addAttendance(@RequestBody @Valid NewAttendanceDTO attendance, BindingResult bindingResult,
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

        Long attendanceId = attendanceService.addAttendance(attendance, loggedUser);
        response.setStatus(HttpServletResponse.SC_CREATED);

        return attendanceId;
    }

    @RequestMapping(value="/attendances/{attendanceId}", method = RequestMethod.DELETE)
    public void deleteAttendance(@PathVariable Long attendanceId) throws NotFoundException, BadRequestException,
            ForbiddenException {
        //Get logged user
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        attendanceService.deleteAttendance(attendanceId, loggedUser);
    }
}
