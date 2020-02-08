package it.polito.ai.pedibusbackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.repositories.*;
import it.polito.ai.pedibusbackend.services.LineService;
import it.polito.ai.pedibusbackend.viewmodels.LineDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.sql.Timestamp;
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
    private NotificationRepository notificationRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private RideRepository rideRepository;

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
                            try {
                                lineService.addLine(lineDTO);
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
                            } catch (Exception e) {
                                log.error("Error adding line " + lineDTO.getName() + " to the DB: " + e.getMessage());
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

        Line line1 = lineRepository.getById(Long.parseLong("1"));
        Line line2 = lineRepository.getById(Long.parseLong("8"));

        log.info("Adding users to the DB...");
        User user;
        ArrayList<String> roles;
        ArrayList<Line> lines;

        //Create some rides
        //Yesterday
        Ride r1 = persistNewRide(new java.sql.Date(System.currentTimeMillis()-24*60*60*1000), line1, 'O', true);
        //Today
        Ride r2 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), line1, 'O', true);
        Ride r3 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), line1, 'R', true);
        //Tomorrow
        Ride r4 = persistNewRide(new java.sql.Date(System.currentTimeMillis()+24*60*60*1000), line1, 'O', true);
        Ride r5 = persistNewRide(new java.sql.Date(System.currentTimeMillis()+24*60*60*1000), line1, 'R', false);

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
        lines.add(line1);
        lines.add(line2);
        user = persistNewUser("user0@email.it", "user0", "user0", roles, lines, "Password0");
        log.info("Created " + user.getEmail());

        //Create User0's pupils
        Pupil p1 = persistNewPupil("Andrea", line1, user);
        Pupil p2 = persistNewPupil("Federico", line1, user);
        Pupil p3 = persistNewPupil("Kamil", line2, user);

        //Create some availabilities
        persistNewAvailability(user, r1, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r2, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r3, line1.getStops().get(3), "CONSOLIDATED");
        persistNewAvailability(user, r4, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r5, line1.getStops().get(3), "ASSIGNED");

        //Create some reservations
        Reservation r = persistNewReservation(p1, r4, line1.getStops().get(0));
        persistNewReservation(p2, r4, line1.getStops().get(1));

        //Create some attendances
        persistNewAttendance(p1, r4, line1.getStops().get(0), r);
        persistNewAttendance(p3, r4, line1.getStops().get(0), null);

        //Create User1
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user1@email.it", "user1", "user1", roles, lines, "Password1");
        log.info("Created " + user.getEmail());

        //Create User1's pupils
        persistNewPupil("Luigi", line1, user);
        persistNewPupil("Mario", line1, user);
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
