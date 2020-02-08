package it.polito.ai.pedibusbackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.repositories.*;
import it.polito.ai.pedibusbackend.services.LineService;
import it.polito.ai.pedibusbackend.services.MailService;
import it.polito.ai.pedibusbackend.services.UserService;
import it.polito.ai.pedibusbackend.viewmodels.LineDTO;
import it.polito.ai.pedibusbackend.viewmodels.NewUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CommandLineAppStartupRunner.class);
    @Autowired
    private LineService lineService;
    @Autowired
    private UserService userService;
    @Autowired
    private MailService mailService;
    @Autowired
    private LineRepository lineRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PupilRepository pupilRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private RideRepository rideRepository;
    @Value("${backend.url}")
    private String serverUrl;

    @Override
    public void run(String...args) {
        log.info("Adding lines to the db...");

        //Find lines dir resource
        URL linesDirURL = this.getClass().getResource("/lines");

        //Check dir existence
        if(linesDirURL == null) {
            log.error("Cannot access lines directory");
        } else {
            File linesDir = new File(linesDirURL.getFile());

            //Check dir existence
            if (!linesDir.isDirectory()) {
                log.error("Cannot access lines directory");
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

                //Retrieve all json files in the dir
                Pattern p = Pattern.compile(".*\\.json");
                File[] linesFiles = linesDir.listFiles((dir, name) -> p.matcher(name).matches());

                //Parse every file
                for (File f: linesFiles) {
                    try {
                        //Read the json into a DTO
                        LineDTO lineDTO = objectMapper.readValue(f, LineDTO.class);

                        //Check DTO validity
                        Set<ConstraintViolation<LineDTO>> violations = validator.validate(lineDTO);
                        if (violations.size() > 0) {
                            StringBuilder err = new StringBuilder("Errors validating file " + f.getName() + ":");
                            for (ConstraintViolation<LineDTO> violation : violations) {
                                err.append("\n" + violation.getPropertyPath() + ": " + violation.getMessage());
                            }
                            log.error(err.toString());
                        } else {
                            //Try to add line and stops to the DB
                            Line addedLine = null;
                            try {
                                addedLine = lineService.addLine(lineDTO);
                                log.info("Line " + lineDTO.getName() + " added to the DB");

                            } catch (Exception e) {
                                log.error("Error adding line " + lineDTO.getName() + " to the DB: " + e.getMessage());
                            }

                            // try to create a new user
                            try{
                                NewUserDTO newUserDTO = new NewUserDTO();
                                newUserDTO.setEmail(lineDTO.getEmail());

                                String uuid = userService.createUser(newUserDTO); //could throw BadRequestException
                                User u = userRepository.findById(lineDTO.getEmail()).orElse(null);

                                // Build registration URL
                                String registerUrl = serverUrl + "register/" + uuid;

                                mailService.sendRegistrationMail(u.getEmail(), registerUrl);

                                //add user roles
                                u.getRoles().add("ROLE_ADMIN");
                                u.getRoles().add("ROLE_USER");
                                //add the line to the user

                                List<Line> lines = new ArrayList<>();
                                lines.add(addedLine);
                                u.setLines(lines);
                                userRepository.save(u);

                            } catch(BadRequestException bre) {   //enter here if user already exist
                                User u = userRepository.findById(lineDTO.getEmail()).orElse(null);
                                //add the line to the user
                                u.getLines().add(addedLine);
                                userRepository.save(u);
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Errors parsing file " + f.getName() + ": " + e.getMessage());
                    } catch (IOException e) {
                        log.error("Unable to open file " + f.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        List<Line> dbLines = lineRepository.findAll();

        log.info("Adding users to the DB...");
        User user;
        ArrayList<String> roles;
        ArrayList<Line> lines;

        //Create some rides
        //Yesterday
        Ride r1 = persistNewRide(new java.sql.Date(System.currentTimeMillis()-24*60*60*1000), dbLines.get(0), 'O', true);
        //Today
        Ride r2 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), dbLines.get(0), 'O', true);
        Ride r3 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), dbLines.get(0), 'R', true);
        //Tomorrow
        Ride r4 = persistNewRide(new java.sql.Date(System.currentTimeMillis()+24*60*60*1000), dbLines.get(0), 'O', true);
        Ride r5 = persistNewRide(new java.sql.Date(System.currentTimeMillis()+24*60*60*1000), dbLines.get(0), 'R', false);

        //Create Admin
        roles = new ArrayList<>();
        roles.add("ROLE_SYSTEM-ADMIN");
        user = persistNewUser("admin@email.it", "admin", "admin", roles, null, "Admin0");
        log.info("Created " + user.getEmail());

        //Create User0
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        lines.add(dbLines.get(0));
        lines.add(dbLines.get(1));
        user = persistNewUser("user0@email.it", "Mario", "Rossi", roles, lines, "Password0");
        log.info("Created " + user.getEmail());

        //Create User0's pupils
        Pupil p1 = persistNewPupil("Andrea", dbLines.get(0), user);
        Pupil p2 = persistNewPupil("Federico", dbLines.get(0), user);
        Pupil p3 = persistNewPupil("Kamil", dbLines.get(1), user);

        //Create some availabilities
        persistNewAvailability(user, r1, dbLines.get(0).getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r2, dbLines.get(0).getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r3, dbLines.get(0).getStops().get(3), "CONSOLIDATED");
        persistNewAvailability(user, r4, dbLines.get(0).getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r5, dbLines.get(0).getStops().get(3), "ASSIGNED");

        //Create some reservations
        Reservation r = persistNewReservation(p1, r4, dbLines.get(0).getStops().get(0));
        persistNewReservation(p2, r4, dbLines.get(0).getStops().get(1));

        //Create some attendances
        persistNewAttendance(p1, r4, dbLines.get(0).getStops().get(0), r);
        persistNewAttendance(p3, r4, dbLines.get(0).getStops().get(0), null);

        //Create User1
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user1@email.it", "Giuseppe", "Verdi", roles, lines, "Password1");
        log.info("Created " + user.getEmail());

        //Create User1's pupils
        persistNewPupil("Luigi", dbLines.get(0), user);
        persistNewPupil("Mario", dbLines.get(0), user);
    }

    private User persistNewUser(String email, String name, String surname, List<String> roles, List<Line> lines, String password) {
        PasswordEncoder passwordEncoder =  new BCryptPasswordEncoder();
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setSurname(surname);
        user.setEnabled(true);
        user.setRoles(roles);
        user.setLines(lines);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private Pupil persistNewPupil(String name, Line line, User user) {
        Pupil p = new Pupil();
        p.setName(name);
        p.setLine(line);
        p.setUser(user);
        return pupilRepository.save(p);
    }

    private Ride persistNewRide(Date date, Line line, Character direction, Boolean consolidated) {
        Ride r = new Ride();
        r.setDate(date);
        r.setLine(line);
        r.setDirection(direction);
        r.setConsolidated(consolidated);
        return rideRepository.save(r);
    }

    private void persistNewAvailability(User user, Ride ride, Stop stop, String status) {
        Availability a = new Availability();
        a.setUser(user);
        a.setRide(ride);
        a.setStop(stop);
        a.setStatus(status);
        availabilityRepository.save(a);
    }

    private Reservation persistNewReservation(Pupil pupil, Ride ride, Stop stop) {
        Reservation r = new Reservation();
        r.setPupil(pupil);
        r.setRide(ride);
        r.setStop(stop);
        return reservationRepository.save(r);
    }

    private Attendance persistNewAttendance(Pupil pupil, Ride ride, Stop stop, Reservation reservation) {
        Attendance a = new Attendance();
        a.setPupil(pupil);
        a.setRide(ride);
        a.setStop(stop);
        a.setReservation(reservation);
        return attendanceRepository.save(a);
    }
}
