package it.polito.ai.pedibusbackend.services;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import it.polito.ai.pedibusbackend.entities.Line;
import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.Ride;
import it.polito.ai.pedibusbackend.entities.Stop;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.LineRepository;
import it.polito.ai.pedibusbackend.repositories.RideRepository;
import it.polito.ai.pedibusbackend.repositories.StopRepository;
import it.polito.ai.pedibusbackend.viewmodels.LineDTO;
import it.polito.ai.pedibusbackend.viewmodels.PupilDTO;
import it.polito.ai.pedibusbackend.viewmodels.RideDTO;
import it.polito.ai.pedibusbackend.viewmodels.StopDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Service
public class LineService{
    private static final Logger log = LoggerFactory.getLogger(LineService.class);
    @Autowired
    private LineRepository lineRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private RideRepository rideRepository;

    public List<LineDTO> getLines() {
        List<LineDTO> lines = new ArrayList<>();
        LineDTO lineDTO;

        for (Line l: lineRepository.findAll()) {
            lineDTO = new LineDTO();
            lineDTO.setId(l.getId());
            lineDTO.setName(l.getName());
            lines.add(lineDTO);
        }

        return lines;
    }

    public LineDTO getLine(Long lineId) throws NotFoundException {
        /* Get requested line */
        Line line = lineRepository.getById(lineId);
        if(line == null) {
            throw new NotFoundException("Line not found");
        }

        LineDTO lineDTO = new LineDTO();
        List<StopDTO> outwardStops = new ArrayList<>();
        List<StopDTO> returnStops = new ArrayList<>();

        /* Map every stop entity into a DTO and add it to the proper list */
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (Stop stop: line.getStops()) {
            StopDTO stopDTO = new StopDTO();

            stopDTO.setId(stop.getId());
            stopDTO.setName(stop.getName());
            stopDTO.setOrder(stop.getOrder());
            stopDTO.setTime(sdf.format(stop.getTime()));

            if(stop.getDirection() == 'O') {
                outwardStops.add(stopDTO);
            } else {
                returnStops.add(stopDTO);
            }
        }

        /* Add stop lists to the line DTO */
        lineDTO.setOutwardStops(outwardStops);
        lineDTO.setReturnStops(returnStops);

        return lineDTO;
    }

    public List<RideDTO> getLineRides(Long lineId, Date date, Character direction)
            throws NotFoundException, BadRequestException {
        List<RideDTO> rides = new ArrayList<>();
        RideDTO rideDTO;

        Line line = lineRepository.getById(lineId);
        if(line == null) {
            throw new NotFoundException("Line not found");
        }

        if(direction != null){
            if(!direction.equals('O')  &&  !direction.equals('R')) {
                throw new BadRequestException("Wrong direction character");
            }
            if(date != null){
                Ride ride = rideRepository.getByLineAndDateAndDirection(line, date, direction).orElse(null);
                if(ride != null){
                    rideDTO = new RideDTO();
                    rideDTO.setId(ride.getId());
                    rideDTO.setDate(ride.getDate());
                    rideDTO.setDirection(ride.getDirection());
                    rideDTO.setConsolidated(ride.getConsolidated());
                    rides.add(rideDTO);
                }
            }
        }
        else{
            for (Ride ride: rideRepository.getByLineOrderByDateAscDirectionAsc(line)) {
                rideDTO = new RideDTO();
                rideDTO.setId(ride.getId());
                rideDTO.setDate(ride.getDate());
                rideDTO.setDirection(ride.getDirection());
                rideDTO.setConsolidated(ride.getConsolidated());

                rides.add(rideDTO);
            }
        }

        return rides;
    }

    public List<PupilDTO> getLinePupils(Long lineId) throws NotFoundException{
        List<PupilDTO> pupils = new ArrayList<>();
        PupilDTO pupilDTO;

        /* Get requested line */
        Line line = lineRepository.getById(lineId);
        if(line == null) {
            throw new NotFoundException("Line not found");
        }

        for (Pupil p: line.getPupils()) {
            pupilDTO = new PupilDTO();
            pupilDTO.setId(p.getId());
            pupilDTO.setName(p.getName());
            pupilDTO.setUserId(p.getUser().getEmail());
            pupils.add(pupilDTO);
        }

        return pupils;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public Line addLine(LineDTO lineDTO) throws ParseException {
        GeometryFactory geoFactory = new GeometryFactory(new PrecisionModel(), 4326);
        SimpleDateFormat sdf = new SimpleDateFormat("H:m");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        /* Map line DTO to entity and persist it */
        Line line = new Line();
        line.setName(lineDTO.getName());
        line = lineRepository.save(line);

        /* Map outward stops DTOs to entities and persist them */
        if (lineDTO.getOutwardStops() != null) {
            for (StopDTO stopDTO: lineDTO.getOutwardStops()) {
                Stop stop = new Stop();
                stop.setName(stopDTO.getName());
                stop.setOrder(stopDTO.getOrder());
                stop.setDirection('O');
                stop.setTime(new Time(sdf.parse(stopDTO.getTime()).getTime()));
                stop.setLocation(geoFactory.createPoint(new Coordinate(stopDTO.getLng(), stopDTO.getLat())));
                stop.setLine(line);

                stopRepository.save(stop);
            }
        }

        /* Map return stops DTOs to entities and persist them */
        if (lineDTO.getReturnStops() != null) {
            for (StopDTO stopDTO: lineDTO.getReturnStops()) {
                Stop stop = new Stop();
                stop.setName(stopDTO.getName());
                stop.setOrder(stopDTO.getOrder());
                stop.setDirection('R');
                stop.setTime(new Time(sdf.parse(stopDTO.getTime()).getTime()));
                stop.setLocation(geoFactory.createPoint(new Coordinate(stopDTO.getLng(), stopDTO.getLat())));
                stop.setLocation(geoFactory.createPoint(new Coordinate(stopDTO.getLng(), stopDTO.getLat())));
                stop.setLine(line);

                stopRepository.save(stop);
            }
        }

        return line;
    }
}
