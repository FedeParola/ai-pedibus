package it.polito.ai.pedibusbackend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

@Service
public class LineService implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(LineService.class);
    @Autowired
    private LineRepository lineRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private RideRepository rideRepository;
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private UserService userService;

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
            throw new NotFoundException("Line " + lineId + " not found");
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
            throw new NotFoundException("Line " + lineId + " not found");
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
            for (Ride ride: rideRepository.getByLine(line)) {
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
            throw new NotFoundException("Line " + lineId + " not found");
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

    @Override
    public void afterPropertiesSet() throws Exception {
        /* Find lines dir resource */
        URL linesDirURL = this.getClass().getResource("/lines");

        /* Check dir existence */
        if(linesDirURL == null) {
            log.error("Cannot access lines directory");

        } else {
            File linesDir = new File(linesDirURL.getFile());

            /* Check dir existence */
            if (!linesDir.isDirectory()) {
                log.error("Cannot access lines directory");

            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

                /* Retrieve all json files in the dir */
                Pattern p = Pattern.compile(".*\\.json");
                File[] linesFiles = linesDir.listFiles((dir, name) -> p.matcher(name).matches());

                /* Parse every file */
                for (File f: linesFiles) {
                    try {
                        /* Read the json into a DTO */
                        LineDTO lineDTO = objectMapper.readValue(f, LineDTO.class);

                        /* Check DTO validity */
                        Set<ConstraintViolation<LineDTO>> violations = validator.validate(lineDTO);
                        if (violations.size() > 0) {
                            StringBuilder err = new StringBuilder("Errors validating file " + f.getName() + ":");
                            for (ConstraintViolation<LineDTO> violation : violations) {
                                err.append("\n" + violation.getPropertyPath() + ": " + violation.getMessage());
                            }
                            log.error(err.toString());

                        } else {
                            /* Try to add line and stops to the DB */
                            try {
                                Line addedLine = addLine(lineDTO);
                                log.info("Line " + lineDTO.getName() + " added to the DB");
                                /*
                                //try to create a new user
                                try{
                                    NewUserDTO newUserDTO = new NewUserDTO();
                                    newUserDTO.setEmail(lineDTO.getEmail());
                                    String uuid = userService.createUser(newUserDTO); //could throw BadRequestException
                                    User u = userRepository.getByEmail(lineDTO.getEmail());

                                    // Build registration URL
                                    String requestUrl = request.getRequestURL().toString();
                                    String registerUrl = requestUrl.substring(0, requestUrl.lastIndexOf(request.getRequestURI())) + "/register/" + uuid;

                                    mailService.sendRegistrationMail(newUser.getEmail(), registerUrl);

                                    //add user roles
                                    u.getRoles().add("ROLE_ADMIN");
                                    u.getRoles().add("ROLE_USER");
                                    //add the line to the user
                                    u.getLines().add(addedLine);
                                    userRepository.save(u);
                                }catch(BadRequestException bre){   //enter here if user already exist
                                    User u = userRepository.getByEmail(lineDTO.getEmail());
                                    //add the line to the user
                                    u.getLines().add(addedLine);
                                    userRepository.save(u);
                                }
                                */
                            } catch(Exception e) {
                                log.error("Error adding line " + lineDTO.getName() + " to the DB: " + e.getMessage());
                            }
                        }

                    } catch (JsonProcessingException e) {
                        log.error("Errors parsing file " + f.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    protected Line addLine(LineDTO lineDTO) throws ParseException {
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
