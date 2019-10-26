package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.LineService;
import it.polito.ai.pedibusbackend.viewmodels.LineDTO;
import it.polito.ai.pedibusbackend.viewmodels.PupilDTO;
import it.polito.ai.pedibusbackend.viewmodels.RideDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;

@RestController
public class LineController {
    @Autowired
    private LineService lineService;

    @RequestMapping(value = "/lines", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LineDTO> getLines() {
        return lineService.getLines();
    }

    @RequestMapping(value = "/lines/{lineId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public LineDTO getLine(@PathVariable Long lineId) throws NotFoundException {
        LineDTO line = lineService.getLine(lineId);

        return line;
    }

    @RequestMapping(value = "/lines/{lineId}/rides", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RideDTO> getLineRides(@PathVariable Long lineId,
                                      @RequestParam Date date,
                                      @RequestParam Character direction)
            throws NotFoundException, BadRequestException {
        return lineService.getLineRides(lineId, date, direction);
    }

    @RequestMapping(value = "/lines/{lineId}/pupils", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PupilDTO> getLinePupils(@PathVariable Long lineId) throws NotFoundException {
        return lineService.getLinePupils(lineId);
    }


}
