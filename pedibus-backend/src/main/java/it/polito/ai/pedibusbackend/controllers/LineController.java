package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.LineService;
import it.polito.ai.pedibusbackend.viewmodels.LineDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class LineController {
    @Autowired
    private LineService lineService;

    @RequestMapping(value = "/lines", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<String> getLines() {
        return lineService.getLineNames();
    }

    @RequestMapping(value = "/lines/{lineName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public LineDTO getLine(@PathVariable String lineName) throws NotFoundException {
        LineDTO line = lineService.getLine(lineName);

        return line;
    }
}
