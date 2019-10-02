package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.*;
import it.polito.ai.pedibusbackend.security.AuthorizationManager;
import it.polito.ai.pedibusbackend.viewmodels.AuthorizationDTO;
import it.polito.ai.pedibusbackend.viewmodels.RegistrationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class UserService implements InitializingBean, UserDetailsService {
    private static final long CONF_TOKEN_EXPIRY_HOURS = 24;
    private static final long RECOVER_TOKEN_EXPIRY_MIN = 30;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LineRepository lineRepository;
    @Autowired
    private PupilRepository pupilRepository;
    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;
    @Autowired
    private RecoverTokenRepository recoverTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public String registerUser(RegistrationDTO registrationDTO) throws BadRequestException {
        /* Check duplicate user */
        User user = userRepository.findById(registrationDTO.getEmail()).orElse(null);
        if (user != null) {
            if (user.isEnabled()) {
                throw new BadRequestException("Email " + registrationDTO.getEmail() + " already registered");

            /* Check if user isn't confirmed and confirmation is expired */
            } else {
                ConfirmationToken token = confirmationTokenRepository.findByUser(user).orElse(null);

                /* User disabled for other reason or waiting for confirmation */
                if (token == null || !token.isExpired()) {
                    throw new BadRequestException("Email " + registrationDTO.getEmail() + " already registered");

                /* Expired token, remove user and token and proceed with registration */
                } else {
                    confirmationTokenRepository.delete(token);
                    userRepository.delete(user);
                }
            }
        }

        /* Store the user in disabled state, with USER role */
        ArrayList<String> roles = new ArrayList<>();
        roles.add("ROLE_USER");
        user = User.builder().email(registrationDTO.getEmail())
                .password(passwordEncoder.encode(registrationDTO.getPass()))
                .enabled(false)
                .roles(roles)
                .build();
        userRepository.save(user);

        /* Necessary, otherwise the system tries to persist the token before the user and returns an error */
        entityManager.flush();

        /* Generate and store confirmation token */
        String uuid = UUID.randomUUID().toString(); // Random UUID
        ConfirmationToken token = new ConfirmationToken();
        token.setUser(user);
        token.setUuid(uuid);
        token.setExpiryDate(new Timestamp(System.currentTimeMillis() + CONF_TOKEN_EXPIRY_HOURS*60*60*1000));
        confirmationTokenRepository.save(token);

        return uuid;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void confirmUser(String uuid) throws NotFoundException {
        ConfirmationToken token = confirmationTokenRepository.findByUuid(uuid).orElseThrow(() -> new NotFoundException());

        User user = token.getUser();

        if (token.isExpired()) {
            /* Delete token and user */
            confirmationTokenRepository.delete(token);
            userRepository.delete(user);

            throw new NotFoundException();
        }

        /* Enable the user */
        user.setEnabled(true);
        userRepository.save(user);

        /* Delete the token */
        confirmationTokenRepository.delete(token);

        return;
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

        return;
    }

    public List<String> getAllUsers(Optional<Integer> page, Optional<Integer> size) throws BadRequestException {
        List<String> users = new ArrayList<>();

        if(page.isPresent() && size.isPresent()){
            for (User u: userRepository.findAll(PageRequest.of(page.get(), size.get()))) {
                users.add(u.getEmail());
            }
        }else if(!page.isPresent() && !size.isPresent()){
            for (User u: userRepository.findAll()) {
                users.add(u.getEmail());
            }
        }else{
            throw new BadRequestException();
        }

        return users;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void grantUser(String userID, AuthorizationDTO authorizationDTO, UserDetails loggedUser) throws BadRequestException, ForbiddenException {
        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());
        User changingUser=userRepository.findById(userID).orElseThrow(() -> new BadRequestException());


        AuthorizationManager.authorizeLineAccess(currentUser, lineRepository.getByName(authorizationDTO.getLineName()));

        //check if not already ADMIN of another Line
        if(!changingUser.getRoles().contains("ROLE_ADMIN")){
            changingUser.getRoles().add("ROLE_ADMIN");
        }
        changingUser.getLines().add(lineRepository.getByName(authorizationDTO.getLineName()));

        userRepository.save(changingUser);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void revokeUser(String userID, AuthorizationDTO authorizationDTO, UserDetails loggedUser)
            throws BadRequestException, ForbiddenException {
        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());
        User changingUser=userRepository.findById(userID).orElseThrow(() -> new BadRequestException());

        AuthorizationManager.authorizeLineAccess(currentUser, lineRepository.getByName(authorizationDTO.getLineName()));

        if(changingUser.getLines().size()==1){
            changingUser.getRoles().remove("ROLE_ADMIN");
        }
        changingUser.getLines().remove(lineRepository.getByName(authorizationDTO.getLineName()));

        userRepository.save(changingUser);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Inizializzazione!");

        Line line1 = lineRepository.getByName("Line1");
        Line line2 = lineRepository.getByName("Line2");
        User user;
        Pupil p;
        ArrayList<String> roles;
        ArrayList<Line> lines;
        ArrayList<Pupil> pupils;

        /* Create Admin */
        roles = new ArrayList<>();
        roles.add("ROLE_SYSTEM-ADMIN");
        persistNewUser("admin@email.it", roles, null, "Admin0");

        /* Create User0 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        lines.add(line1);
        user = persistNewUser("user0@email.it", roles, lines, "Password0");

        /* Create User0's pupils */
        persistNewPupil("Andrea", line1, user);
        persistNewPupil("Federico", line1, user);
        persistNewPupil("Kamil", line1, user);

        /* Create User1 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user1@email.it", roles, lines, "Password1");

        /* Create User1's pupils */
        persistNewPupil("Luigi", line1, user);
        persistNewPupil("Mario", line1, user);

        /* Create User2 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        lines.add(line2);
        user = persistNewUser("user2@email.it", roles, lines, "Password2");

        /* Create User2's pupils */
        persistNewPupil("Giovanni", line2, user);
        persistNewPupil("Piero", line2, user);
        persistNewPupil("Anna", line2, user);

        /* Create User3 */
        roles = new ArrayList<>();
        lines = new ArrayList<>();
        roles.add("ROLE_USER");
        user = persistNewUser("user3@email.it", roles, lines, "Password3");

        /* Create User0's pupils */
        persistNewPupil("Massimo", line2, user);
        persistNewPupil("Giorgia", line2, user);

    }

    User persistNewUser(String email, List<String> roles, List<Line> lines, String password) {
        User user = User.builder()
                .email(email)
                .enabled(true)
                .roles(roles)
                .lines(lines)
                .password(passwordEncoder.encode(password))
                .build();
        return userRepository.save(user);
    }

    void persistNewPupil(String name, Line line, User user) {
        Pupil p = new Pupil();
        p.setName(name);
        p.setLine(line);
        p.setUser(user);
        pupilRepository.save(p);
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userRepository.findById(s).orElseThrow(() -> new UsernameNotFoundException("Email " + s + " not found"));

        /* Retrieve list of authorities from roles */
        List<SimpleGrantedAuthority> authList = user.getRoles().stream()
                                                               .map((r) -> new SimpleGrantedAuthority(r))
                                                               .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), user.isEnabled(),
                true, true, true, authList);
    }

    public String getUser(String userID) throws NotFoundException {
        User u = userRepository.findById(userID).orElse(null);
        if(u != null)
            return u.getEmail();

        throw new NotFoundException();
    }
}
