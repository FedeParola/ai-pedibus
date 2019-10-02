package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.AttendanceService;
import it.polito.ai.pedibusbackend.viewmodels.AttendanceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AttendanceController {
    @Autowired
    private AttendanceService attendanceService;

    @RequestMapping(value = "/attendances/{lineName}/{date}", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> createAttendance(@PathVariable String lineName, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
                                               @RequestBody @Valid AttendanceDTO attendanceDTO, BindingResult bindingResult,
                                               HttpServletResponse response) throws BadRequestException, NotFoundException {
        Map<String, Long> responseBody = new HashMap<>();

        if(bindingResult.hasErrors()) {
            StringBuilder errMsg = new StringBuilder("Invalid format of the request body:");
            for (FieldError err : bindingResult.getFieldErrors()) {
                errMsg.append(" " + err.getField() + ": " + err.getDefaultMessage() + ";");
            }
            throw new BadRequestException(errMsg.toString());
        }

        Long attendanceId = attendanceService.addAttendance(attendanceDTO, lineName, date);
        response.setStatus(HttpServletResponse.SC_CREATED);

        responseBody.put("Id", attendanceId);
        return responseBody;
    }

    @RequestMapping(value = "/attendances/{lineName}/{date}/{attendanceId}", method = RequestMethod.DELETE)
    public void deleteAttendance(@PathVariable String lineName, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
                                  @PathVariable Long attendanceId) throws NotFoundException {

        attendanceService.deleteAttendance(attendanceId, lineName, date);
        return;
    }
}
