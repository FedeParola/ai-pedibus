package it.polito.ai.pedibusbackend;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CommandLineAppStartupRunner.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LineRepository lineRepository;
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
        log.info("Adding users to the DB...");
        Line line1 = lineRepository.getById(Long.parseLong("1"));
        Line line2 = lineRepository.getById(Long.parseLong("8"));
        User user;
        ArrayList<String> roles;
        ArrayList<Line> lines;

        /* Create User0 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        lines.add(line1);
        lines.add(line2);
        user = persistNewUser("user0@email.it", "user0", "user0", roles, lines, "Password0");
        log.info("Created " + user.getEmail());

        /* Create User0's pupils */
        Pupil p1 = persistNewPupil("Andrea", line1, user);
        Pupil p2 = persistNewPupil("Federico", line1, user);
        Pupil p3 = persistNewPupil("Kamil", line1, user);
        Pupil p4 = persistNewPupil("Mario", line2, user);
        Pupil p5 = persistNewPupil("Marco", line2, user);
        Pupil p6 = persistNewPupil("Luigi", line1, user);
        Pupil p7 = persistNewPupil("Pietro", line2, user);

        // Create some notifications for User0
        persistNewNotification(user, "Notification1", "This is the message of notification1!", false, new java.sql.Timestamp(System.currentTimeMillis()));
        persistNewNotification(user, "Notification2", "This is the message of notification2!", false, new java.sql.Timestamp(System.currentTimeMillis()));
        persistNewNotification(user, "Notification3", "This is the message of notification3!", false, new java.sql.Timestamp(System.currentTimeMillis()));
        persistNewNotification(user, "Notification4", "This is the message of notification4!", true, new java.sql.Timestamp(System.currentTimeMillis()));
        persistNewNotification(user, "Notification5", "This is the message of notification5!", false, new java.sql.Timestamp(System.currentTimeMillis()));
        persistNewNotification(user, "Notification6", "This is the message of notification6!", false, new java.sql.Timestamp(System.currentTimeMillis()));
        persistNewNotification(user, "Notification7", "This is the message of notification7!", false, new java.sql.Timestamp(System.currentTimeMillis()));

        /* Create some rides for yesterday */
        Ride r1 = persistNewRide(new java.sql.Date(System.currentTimeMillis()-24*60*60*1000), line1, 'O', true);

        /* Create some rides for today */
        Ride r2 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), line1, 'O', true);
        Ride r3 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), line1, 'R', true);

        /* Create some rides for tomorrow */
        Ride r4 = persistNewRide(new java.sql.Date(System.currentTimeMillis()+24*60*60*1000), line1, 'O', true);
        Ride r5 = persistNewRide(new java.sql.Date(System.currentTimeMillis()+24*60*60*1000), line1, 'R', false);

        /* Create some availabilities */
        persistNewAvailability(user, r1, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r2, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r3, line1.getStops().get(3), "CONSOLIDATED");
        persistNewAvailability(user, r4, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r5, line1.getStops().get(3), "ASSIGNED");

        /* Create some reservations */
        Reservation r = persistNewReservation(p1, r4, line1.getStops().get(0));
        persistNewReservation(p2, r4, line1.getStops().get(1));

        /* Create some attendances */
        persistNewAttendance(p1, r4, line1.getStops().get(0), r);
        persistNewAttendance(p3, r4, line1.getStops().get(0), null);

        /* Create User1 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user1@email.it", "user1", "user1", roles, lines, "Password1");
        log.info("Created " + user.getEmail());

        /* Create User1's pupils */
        persistNewPupil("Luigi", line1, user);
        persistNewPupil("Mario", line1, user);

        /* Create User2 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        lines.add(line1);
        //lines.add(line2);
        user = persistNewUser("user2@email.it", "user2", "user2", roles, lines, "Password2");
        log.info("Created " + user.getEmail());

        /* Create User2's pupils */
        persistNewPupil("Giovanni", line2, user);
        persistNewPupil("Piero", line2, user);
        persistNewPupil("Anna", line2, user);

        /* Create User3 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user3@email.it", "user3", "user3", roles, lines, "Password3");
        log.info("Created " + user.getEmail());

        /* Create User4 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user4@email.it", "user4", "user4", roles, lines, "Password4");
        log.info("Created " + user.getEmail());

        /* Create User5 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user5@email.it", "user5", "user5", roles, lines, "Password5");
        log.info("Created " + user.getEmail());

        /* Create User5's pupils */
        persistNewPupil("Massimo", line2, user);
        persistNewPupil("Giorgia", line2, user);
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

    private void persistNewNotification(User user, String title, String message, Boolean read, Timestamp timestamp) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setRead(read);
        n.setTimestamp(timestamp);
        notificationRepository.save(n);
    }
}
