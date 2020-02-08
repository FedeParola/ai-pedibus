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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService implements UserDetailsService {
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
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private SimpMessagingTemplate msgTemplate;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void register(String uuid, RegistrationDTO registrationDTO) throws NotFoundException {
        RegistrationToken token = registrationTokenRepository.findByUuid(uuid).orElseThrow(
                () -> new NotFoundException("Registration token not found"));

        User user = token.getUser();

        if (token.isExpired()) {
            /* Delete token and user */
            registrationTokenRepository.delete(token);
            userRepository.delete(user);

            throw new NotFoundException("Registration time expired");
        }

        /* Save data and enable the user */
        user.setName(registrationDTO.getName());
        user.setSurname(registrationDTO.getSurname());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setEnabled(true);

        msgTemplate.convertAndSend("/topic/users", "");

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
            throw new NotFoundException("Password recovery time expired");

        } else {
            User user = token.getUser();
            user.setPassword(passwordEncoder.encode(newPass));
            userRepository.save(user);
            recoverTokenRepository.delete(token);
        }
    }

    public List<UserDTO> getUsers(Optional<Integer> page, Optional<Integer> size) throws BadRequestException {
        List<UserDTO> users = new ArrayList<>();
        List<User> requestedUsers = new ArrayList<>();

        if(page.isPresent() && size.isPresent()){
            requestedUsers = userRepository.findAll(PageRequest.of(page.get(), size.get())).getContent()
                    .stream()
                    .collect(Collectors.toList());
        }else if(!page.isPresent() && !size.isPresent()){
            requestedUsers = StreamSupport
                    .stream(userRepository.findAll().spliterator(), false)
                    .collect(Collectors.toList());
        }else{
            throw new BadRequestException("Invalid request parameters");
        }

        for (User u : requestedUsers) {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail(u.getEmail());
            userDTO.setEnabled(u.isEnabled());
            userDTO.setName(u.getName());
            userDTO.setSurname(u.getSurname());
            userDTO.setRoles(new ArrayList<>(u.getRoles()));
            List<LineDTO> lines = new ArrayList<>();
            for (Line l : u.getLines()) {
                LineDTO lineDTO = new LineDTO();
                lineDTO.setId(l.getId());
                lineDTO.setName(l.getName());
                lines.add(lineDTO);
            }
            userDTO.setLines(lines);
            users.add(userDTO);
        }

        //set to the last user if is the last
        if(page.isPresent()){
            if(userRepository.findAll(PageRequest.of(page.get() + 1, size.get())).isEmpty()){
                users.get(requestedUsers.size() - 1).setHasNext(false);
            }else{
                users.get(requestedUsers.size() -1).setHasNext(true);
            }
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

        //
        msgTemplate.convertAndSend("/topic/users", "");

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

    public UserDTO getUser(String userId, String loggedUserId, Optional<Boolean> check)
            throws NotFoundException, ForbiddenException, BadRequestException {
        User user;
        UserDTO userDTO = new UserDTO();;

        //if we are only checking if the user exists
        if(check.isPresent() && check.get().equals(true)){
            user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
            userDTO.setEmail(user.getEmail());
            return userDTO;
        }

        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException("The user is not allowed to do this action");
        }

        user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        /* Build result structure */
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
            throw new ForbiddenException("The user is not allowed to do this action");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

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
            throw new ForbiddenException("The user is not allowed to do this action");
        }

        /* Retrieve user and line entities */
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Line line = lineRepository.findById(lineId).orElseThrow(() -> new BadRequestException("Unknown line"));

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

    public List<LineDTO> getAdminLines(String userId) throws NotFoundException {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        List<LineDTO> lines = new ArrayList<>();
        LineDTO lineDTO;

        for(Line l: user.getLines()){
            lineDTO = new LineDTO();
            lineDTO.setId(l.getId());
            lineDTO.setName(l.getName());

            lines.add(lineDTO);
        }

        return lines;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void addAdminLines(String userId, Set<Long> lineIds, String loggedUserId)
            throws BadRequestException, NotFoundException, ForbiddenException {
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        if (lineIds.size() == 0) {
            return;
        }

        List<Long> currentLinesIds = user.getLines().stream().map(Line::getId).collect(Collectors.toList());

        /* Get lines entities */
        List<Line> lines = new ArrayList<>();
        for (Long lineId : lineIds) {
            if (!currentLinesIds.contains(lineId)) {
                lines.add(lineRepository.findById(lineId).orElseThrow(() -> new BadRequestException("Unknown line")));
            }
        }

        AuthorizationManager.authorizeLinesAccess(loggedUser, lineIds);

        if (!user.getRoles().contains("ROLE_ADMIN")) {
            user.getRoles().add("ROLE_ADMIN");
        }

        user.getLines().addAll(lines);

        notificationService.createNotification(user, "Admin of a new line", "You are now admin of the following " +
                "line: '" + lines.get(0).getName() + "'");
        msgTemplate.convertAndSend("/topic/users", "");
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void removeAdminLine(String userId, Long lineId, String loggedUserId)
            throws BadRequestException, NotFoundException, ForbiddenException {
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Line line = lineRepository.findById(lineId).orElseThrow(() -> new NotFoundException("Line not found"));

        AuthorizationManager.authorizeLineAccess(loggedUser, lineId);

        if (!user.getLines().removeIf(l -> l.getId().equals(lineId))) {
            throw new NotFoundException("Line not found");
        }

        if(user.getLines().size() == 0) {
            user.getRoles().remove("ROLE_ADMIN");
        }

        notificationService.createNotification(user, "Removed as admin of a line", "You are no longer admin of the following " +
                "line: '" + line.getName() + "'");
        msgTemplate.convertAndSend("/topic/users", "");
    }

    public List<PupilDTO> getPupils(String userId, Optional<Integer> page, Optional<Integer> size, String loggedUserId)
            throws ForbiddenException, NotFoundException, BadRequestException {
        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException("The user is not allowed to do this action");
        }
        /* Retrieve user */
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        List<PupilDTO> pupils = new ArrayList<>();
        List<Pupil> requestedPupils = new ArrayList<>();

        if(page.isPresent() && size.isPresent()){
            requestedPupils = pupilRepository.findByUser(user, PageRequest.of(page.get(), size.get(), Sort.by("name"))).getContent()
                    .stream()
                    .collect(Collectors.toList());
        }else if(!page.isPresent() && !size.isPresent()){
            requestedPupils = user.getPupils();
        }else{
            throw new BadRequestException("Invalid request parameters");
        }

        for (Pupil p : requestedPupils) {
            PupilDTO pupilDTO = new PupilDTO();
            pupilDTO.setId(p.getId());
            pupilDTO.setName(p.getName());
            pupilDTO.setLineId(p.getLine().getId());
            pupilDTO.setLineName(p.getLine().getName());

            pupils.add(pupilDTO);
        }

        //set to the last pupil if is the last
        if(page.isPresent() && requestedPupils.size()>0){
            if(pupilRepository.findByUser(user,PageRequest.of(page.get() + 1, size.get(), Sort.by("name"))).isEmpty()){
                pupils.get(requestedPupils.size() - 1).setHasNext(false);
            }else{
                pupils.get(requestedPupils.size() -1).setHasNext(true);
            }
        }

        return pupils;
    }

    public List<RideDTO> getRides(String userId, Long lineId, String loggedUserId)
            throws BadRequestException, ForbiddenException, NotFoundException {
        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException("The user is not allowed to do this action");
        }

        /* Retrieve user and line entities */
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Line line = lineRepository.findById(lineId).orElseThrow(() -> new BadRequestException("Unknown line"));

        /* Retrieve rides */
        Iterable<Ride> rides = rideRepository.getByEscortAndLine(user, line);

        /* Build result structure */
        List<RideDTO> rideDTOs = new ArrayList<>();
        for (Ride r : rides) {
            RideDTO rideDTO = new RideDTO();
            rideDTO.setId(r.getId());
            rideDTO.setDate(r.getDate());
            rideDTO.setDirection(r.getDirection());
            rideDTOs.add(rideDTO);
        }

        return rideDTOs;
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

    public List<NotificationDTO> getNotifications(Optional<Integer> page, Optional<Integer> size, String userId, String name) throws
            BadRequestException, ForbiddenException, NotFoundException {
        List<NotificationDTO> notifications = new ArrayList<>();
        List<Notification> requestedNotifications = new ArrayList<>();

        /* Authorize access */
        User loggedUser = userRepository.findById(name).orElseThrow(BadRequestException::new);
        if (!(name.equals(userId) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException("The user is not allowed to do this action");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        if(page.isPresent() && size.isPresent()){
            requestedNotifications = notificationRepository.findByUser(user, PageRequest.of(page.get(), size.get(), Sort.by(Sort.Direction.DESC, "timestamp"))).getContent()
                    .stream()
                    .collect(Collectors.toList());
        }else if(!page.isPresent() && !size.isPresent()){
            requestedNotifications = user.getNotifications();
        }else{
            throw new BadRequestException("Invalid request parameters");
        }

        for (Notification n : requestedNotifications) {
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setId(n.getId());
            notificationDTO.setMessage(n.getMessage());
            notificationDTO.setRead(n.getRead());
            notificationDTO.setDate(new java.util.Date(n.getTimestamp().getTime()));
            notificationDTO.setTitle(n.getTitle());
            notifications.add(notificationDTO);
        }

        //set to the last user if is the last
        if(page.isPresent() && requestedNotifications.size()>0){
            if(notificationRepository.findByUser(user, PageRequest.of(page.get() + 1, size.get(), Sort.by("timestamp"))).isEmpty()){
                notifications.get(requestedNotifications.size() - 1).setHasNext(false);
            }else{
                notifications.get(requestedNotifications.size() -1).setHasNext(true);
            }
        }

        return notifications;
    }
}
