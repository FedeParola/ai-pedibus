package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.*;
import it.polito.ai.pedibusbackend.security.AuthorizationManager;
import it.polito.ai.pedibusbackend.viewmodels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@DependsOn("lineService")
public class UserService implements InitializingBean, UserDetailsService {
    private static final long CONF_TOKEN_EXPIRY_HOURS = 24;
    private static final long RECOVER_TOKEN_EXPIRY_MIN = 30;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LineRepository lineRepository;
    @Autowired
    private PupilRepository pupilRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private RegistrationTokenRepository registrationTokenRepository;
    @Autowired
    private RecoverTokenRepository recoverTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;


    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void register(String uuid, RegistrationDTO registrationDTO) throws NotFoundException {
        RegistrationToken token = registrationTokenRepository.findByUuid(uuid).orElseThrow(NotFoundException::new);

        User user = token.getUser();

        if (token.isExpired()) {
            /* Delete token and user */
            registrationTokenRepository.delete(token);
            userRepository.delete(user);

            throw new NotFoundException();
        }

        /* Save data and enable the user */
        user.setName(registrationDTO.getName());
        user.setSurname(registrationDTO.getSurname());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setEnabled(true);

        /* Delete the token */
        registrationTokenRepository.delete(token);
    }

    public String createRecoverToken(String email) {
        String uuid = UUID.randomUUID().toString();

        RecoverToken token = new RecoverToken();
        User u = userRepository.findById(email).orElse(null);

        if(u == null)
        {
            return null;
        }

        token.setUser(u);
        token.setUuid(uuid);
        token.setExpiryDate(new Timestamp(System.currentTimeMillis() + RECOVER_TOKEN_EXPIRY_MIN*60*1000));
        recoverTokenRepository.save(token);

        return uuid;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void tryChangePassword(String UUID, String newPass) throws NotFoundException {
        RecoverToken token = recoverTokenRepository.findByUuid(UUID)
                .orElseThrow(()->new NotFoundException("Recover token with UUID " + UUID + " doesn't exist!"));

        if (token.isExpired()) {
            recoverTokenRepository.delete(token);
            throw new NotFoundException("Recover token with UUID " + UUID + " has expired!");

        } else {
            User user = token.getUser();
            user.setPassword(passwordEncoder.encode(newPass));
            userRepository.save(user);
            recoverTokenRepository.delete(token);
        }
    }

    public List<UserDTO> getUsers() {
        List<UserDTO> users = new ArrayList<>();

        for (User u : userRepository.findAll()) {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail(u.getEmail());
            userDTO.setEnabled(u.isEnabled());
            userDTO.setRoles(new ArrayList<>(u.getRoles()));
            List<UserDTO.Line> lines = new ArrayList<>();
            for (Line l : u.getLines()) {
                lines.add(new UserDTO.Line(l.getId(), l.getName()));
            }
            userDTO.setLines(lines);
            users.add(userDTO);
        }

        return users;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public String createUser(NewUserDTO newUser) throws BadRequestException {
        /* Check duplicate user */
        User user = userRepository.findById(newUser.getEmail()).orElse(null);
        if (user != null) {
            if (user.isEnabled()) {
                throw new BadRequestException("Email " + newUser.getEmail() + " already registered");

            } else {
                /* Check if user hasn't completed the registration and the token is expired */
                RegistrationToken token = registrationTokenRepository.findByUser(user).orElse(null);

                /* User disabled for other reason or waiting for confirmation */
                if (token == null || !token.isExpired()) {
                    throw new BadRequestException("Email " + newUser.getEmail() + " already registered");

                    /* Expired token, remove user and token and proceed with creation */
                } else {
                    registrationTokenRepository.delete(token);
                    userRepository.delete(user);
                }
            }
        }

        /* Store the user in disabled state, with USER role */
        user = new User();
        user.setEmail(newUser.getEmail());
        user.setEnabled(false);
        user.getRoles().add("ROLE_USER");
        userRepository.save(user);

        /* Necessary, otherwise the system tries to persist the token before the user and returns an error */
        entityManager.flush();

        /* Generate and store registration token */
        String uuid = UUID.randomUUID().toString(); // Random UUID
        RegistrationToken token = new RegistrationToken();
        token.setUser(user);
        token.setUuid(uuid);
        token.setExpiryDate(new Timestamp(System.currentTimeMillis() + CONF_TOKEN_EXPIRY_HOURS*60*60*1000));
        registrationTokenRepository.save(token);

        return uuid;
    }

    public UserDTO getUser(String userId, String loggedUserId)
            throws NotFoundException, ForbiddenException, BadRequestException {
        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        /* Build result structure */
        UserDTO userDTO = new UserDTO();
        userDTO.setName(user.getName());
        userDTO.setSurname(user.getSurname());

        return userDTO;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void updateUser(String userId, UserUpdateDTO userUpdate, String loggedUserId)
            throws ForbiddenException, NotFoundException, BadRequestException {
        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        /* Update only received fields */
        if (userUpdate.getName() != null) {
            user.setName(userUpdate.getName());
        }
        if (userUpdate.getSurname() != null) {
            user.setSurname(userUpdate.getSurname());
        }
        if (userUpdate.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userUpdate.getPassword()));
        }
    }

    public List<AvailabilityDTO> getAvailabilities(String userId, Boolean consolidatedOnly, Long lineId, String loggedUserId)
            throws BadRequestException, ForbiddenException, NotFoundException {
        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        /* Retrieve user and line entities */
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        Line line = lineRepository.findById(lineId).orElseThrow(BadRequestException::new);

        /* Retrieve availabilities according to query params */
        Iterable<Availability> availabilities;
        if (consolidatedOnly) {
            availabilities = availabilityRepository.findByUserAndLineAndStatus(user, line, "CONSOLIDATED");
        } else {
            availabilities = availabilityRepository.findByUserAndLine(user, line);
        }

        /* Build result structure */
        List<AvailabilityDTO> availabilityDTOs = new ArrayList<>();
        for (Availability a : availabilities) {
            AvailabilityDTO availabilityDTO = new AvailabilityDTO();
            availabilityDTO.setId(a.getId());
            RideDTO rideDTO = new RideDTO();
            rideDTO.setId(a.getRide().getId());
            rideDTO.setDirection(a.getRide().getDirection());
            rideDTO.setDate(a.getRide().getDate());
            availabilityDTO.setRide(rideDTO);
            availabilityDTO.setStopId(a.getStop().getId());
            availabilityDTO.setStatus(a.getStatus());
            availabilityDTOs.add(availabilityDTO);
        }

        return availabilityDTOs;
    }

    public List<Long> getAdminLines(String userId) throws NotFoundException {
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        return user.getLines().stream().map(Line::getId).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void addAdminLines(String userId, Set<Long> lineIds, String loggedUserId)
            throws BadRequestException, NotFoundException, ForbiddenException {
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        if (lineIds.size() == 0) {
            return;
        }

        List<Long> currentLinesIds = user.getLines().stream().map(Line::getId).collect(Collectors.toList());

        /* Get lines entities */
        List<Line> lines = new ArrayList<>();
        for (Long lineId : lineIds) {
            if (!currentLinesIds.contains(lineId)) {
                lines.add(lineRepository.findById(lineId).orElseThrow(BadRequestException::new));
            }
        }

        AuthorizationManager.authorizeLinesAccess(loggedUser, lineIds);

        if (!user.getRoles().contains("ROLE_ADMIN")) {
            user.getRoles().add("ROLE_ADMIN");
        }

        user.getLines().addAll(lines);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void removeAdminLine(String userId, Long lineId, String loggedUserId)
            throws BadRequestException, NotFoundException, ForbiddenException {
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        AuthorizationManager.authorizeLineAccess(loggedUser, lineId);

        if (!user.getLines().removeIf(l -> l.getId().equals(lineId))) {
            throw new NotFoundException();
        }

        if(user.getLines().size() == 0) {
            user.getRoles().remove("ROLE_ADMIN");
        }
    }

    public List<PupilDTO> getPupils(String userId, String loggedUserId)
            throws ForbiddenException, NotFoundException, BadRequestException {
        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        /* Retrieve user */
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        /* Build result structure */
        List<PupilDTO> pupilDTOs = new ArrayList<>();
        for (Pupil p : user.getPupils()) {
            PupilDTO pupilDTO = new PupilDTO();
            pupilDTO.setId(p.getId());
            pupilDTO.setName(p.getName());
            pupilDTO.setLineId(p.getLine().getId());
            pupilDTOs.add(pupilDTO);
        }

        return pupilDTOs;
    }

    @Override
    public void afterPropertiesSet() {
        Line line1 = lineRepository.getById(Long.parseLong("1"));
        Line line2 = lineRepository.getById(Long.parseLong("8"));
        User user;
        ArrayList<String> roles;
        ArrayList<Line> lines;

        /* Create Admin */
        roles = new ArrayList<>();
        roles.add("ROLE_SYSTEM-ADMIN");
        persistNewUser("admin@email.it", "admin", "admin", roles, null, "Admin0");

        /* Create User0 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        lines.add(line1);
        lines.add(line2);
        user = persistNewUser("user0@email.it", "user0", "user0", roles, lines, "Password0");

        /* Create User0's pupils */
        persistNewPupil("Andrea", line1, user);
        persistNewPupil("Federico", line1, user);
        persistNewPupil("Kamil", line1, user);

        /* Create some rides for yesterday */
        Ride r1 = persistNewRide(new java.sql.Date(System.currentTimeMillis()-24*60*60*1000), line1, 'O', true);
        Ride r2 = persistNewRide(new java.sql.Date(System.currentTimeMillis()-24*60*60*1000), line1, 'R', true);

        /* Create some rides for today */
        Ride r3 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), line1, 'O', true);
        Ride r4 = persistNewRide(new java.sql.Date(System.currentTimeMillis()), line1, 'R', true);

        /* Create some rides for tomorrow */
        Ride r5 = persistNewRide(new java.sql.Date(System.currentTimeMillis()+24*60*60*1000), line1, 'O', true);

        /* Create some availabilities */
        persistNewAvailability(user, r1, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r2, line1.getStops().get(2), "CONSOLIDATED");
        persistNewAvailability(user, r3, line1.getStops().get(0), "CONSOLIDATED");
        persistNewAvailability(user, r4, line1.getStops().get(2), "CONSOLIDATED");
        persistNewAvailability(user, r5, line1.getStops().get(0), "CONSOLIDATED");

        /* Create User1 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user1@email.it", "user1", "user1", roles, lines, "Password1");

        /* Create User1's pupils */
        persistNewPupil("Luigi", line1, user);
        persistNewPupil("Mario", line1, user);

        /* Create User2 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        lines.add(line2);
        user = persistNewUser("user2@email.it", "user2", "user2", roles, lines, "Password2");

        /* Create User2's pupils */
        persistNewPupil("Giovanni", line2, user);
        persistNewPupil("Piero", line2, user);
        persistNewPupil("Anna", line2, user);

        /* Create User3 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user3@email.it", "user3", "user3", roles, lines, "Password3");

        /* Create User0's pupils */
        persistNewPupil("Massimo", line2, user);
        persistNewPupil("Giorgia", line2, user);
    }

    private User persistNewUser(String email, String name, String surname, List<String> roles, List<Line> lines, String password) {
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

    private void persistNewPupil(String name, Line line, User user) {
        Pupil p = new Pupil();
        p.setName(name);
        p.setLine(line);
        p.setUser(user);
        pupilRepository.save(p);
    }

    private Ride persistNewRide(Date date, Line line, Character direction, Boolean consolidated){
        Ride r = new Ride();
        r.setDate(date);
        r.setLine(line);
        r.setDirection(direction);
        r.setConsolidated(consolidated);
        return rideRepository.save(r);
    }

    private void persistNewAvailability(User user, Ride ride, Stop stop, String status){
        Availability a = new Availability();
        a.setUser(user);
        a.setRide(ride);
        a.setStop(stop);
        a.setStatus(status);
        availabilityRepository.save(a);
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userRepository.findById(s).orElseThrow(() -> new UsernameNotFoundException("Email " + s + " not found"));

        /* Retrieve list of authorities from roles */
        List<SimpleGrantedAuthority> authList = user.getRoles().stream()
                                                               .map(SimpleGrantedAuthority::new)
                                                               .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), user.isEnabled(),
                true, true, true, authList);
    }
}
