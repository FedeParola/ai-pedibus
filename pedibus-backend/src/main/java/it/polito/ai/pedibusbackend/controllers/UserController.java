package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.security.jwt.JwtTokenProvider;
import it.polito.ai.pedibusbackend.services.MailService;
import it.polito.ai.pedibusbackend.services.UserService;
import it.polito.ai.pedibusbackend.viewmodels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class UserController {
    private static final Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).{6,32}$");
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private MailService mailService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping(value = "/login", consumes= MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> login(@RequestBody @Valid LoginDTO loginDTO) throws BadRequestException {
        String email = loginDTO.getEmail();
        String password = loginDTO.getPassword();

        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            List<String> roles = auth.getAuthorities().stream()
                                                      .map((GrantedAuthority a) -> a.getAuthority())
                                                      .collect(Collectors.toList());
            String token = jwtTokenProvider.createToken(email, roles);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);

            return response;

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email/password supplied");
        }
    }

    @GetMapping(value = "/register/{uuid}")
    public ModelAndView getRegistrationForm(@PathVariable String uuid) {
        ModelAndView response = new ModelAndView();
        response.getModel().put("uuid", uuid);
        response.setViewName("registrationForm");

        return response;
    }

    @PostMapping(value = "/register/{uuid}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ModelAndView register(@PathVariable String uuid,
                                 @Valid RegistrationDTO registrationDTO,
                                 BindingResult res) throws NotFoundException {
        ModelAndView response = new ModelAndView();

        /* Check form correctness */
        if (res.hasErrors() || !registrationDTO.getPassword().equals(registrationDTO.getPasswordConf())) {

            /* Send back registration form with error messages */
            if (res.getFieldErrors("name").size() > 0 ) {
                response.getModel().put("errName", "Missing field");
            }
            if (res.getFieldErrors("surname").size() > 0) {
                response.getModel().put("errSurname", "Missing field");
            }
            if (res.getFieldErrors("password").size() > 0) {
                response.getModel().put("errPassword", "The password must contain at least one uppercase," +
                        "one lowercase character and a digit and be at least 6 characters long");
            }
            if (!registrationDTO.getPassword().equals(registrationDTO.getPasswordConf())) {
                response.getModel().put("errPasswordConf", "Passwords not corresponding");
            }

            response.getModel().put("name", registrationDTO.getName());
            response.getModel().put("surname", registrationDTO.getSurname());
            response.getModel().put("uuid", uuid);
            response.setViewName("registrationForm");

        } else {
            /* Register user */
            userService.register(uuid, registrationDTO);
            response.setViewName("registrationCompleted");
        }

        return response;
    }

    @PostMapping(value = "/recover", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void recover(@RequestBody Map<String, String> emailMap, HttpServletRequest request){

        String email = emailMap.get("email");
        String uuid = userService.createRecoverToken(email);
        if(uuid != null)
        {
            String requestUrl = request.getRequestURL().toString();
            String recoverUrl = requestUrl.substring(0, requestUrl.lastIndexOf(request.getRequestURI())) + "/recover/" + uuid;
            mailService.sendRecoverMail(email, recoverUrl);
        }

        return;
    }

    @GetMapping(value = "/recover/{randomUUID}", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getPassChangeForm(@PathVariable String randomUUID){
        ModelAndView response = new ModelAndView();
        response.getModel().put("randomUUID", randomUUID);
        response.setViewName("passChangeForm");
        return response;
    }

    @PostMapping(value = "/recover/{randomUUID}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void postNewPass(RecoverDTO recoverDTO, @PathVariable String randomUUID) throws NotFoundException {
        String newPass = recoverDTO.getPass();
        String confPass = recoverDTO.getConfPass();
        Matcher m = pattern.matcher(newPass);

        if (newPass == null || confPass == null || !(newPass.equals(recoverDTO.getConfPass()) && m.matches())) {
            throw new NotFoundException();
        }

        userService.tryChangePassword(randomUUID, newPass);
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserDTO> getUsers(@RequestParam("page") Optional<Integer> page, @RequestParam("size") Optional<Integer> size)
            throws BadRequestException{
        return userService.getUsers(page, size);
    }

    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void createUser(@RequestBody NewUserDTO newUser, HttpServletRequest request) throws BadRequestException {
        String uuid = userService.createUser(newUser);

        /* Build registration URL */
        String requestUrl = request.getRequestURL().toString();
        String registerUrl = requestUrl.substring(0, requestUrl.lastIndexOf(request.getRequestURI())) + "/register/" + uuid;

        mailService.sendRegistrationMail(newUser.getEmail(), registerUrl);

        return;
    }

    @GetMapping(value = "/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDTO getUser(@PathVariable String userId, @RequestParam("check") Optional<Boolean> check)
            throws NotFoundException, ForbiddenException, BadRequestException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userService.getUser(userId, loggedUser.getUsername(), check);
    }

    @PutMapping(value = "/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateUser(@PathVariable String userId, @RequestBody @Valid UserUpdateDTO userUpdate)
            throws BadRequestException, NotFoundException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        userService.updateUser(userId, userUpdate, loggedUser.getUsername());

        return;
    }

    @GetMapping(value = "users/{userId}/availabilities", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AvailabilityDTO> getAvailabilities(@PathVariable String userId,
                                                   @RequestParam Boolean consolidatedOnly,
                                                   @RequestParam Long lineId)
            throws BadRequestException, NotFoundException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userService.getAvailabilities(userId, consolidatedOnly, lineId, loggedUser.getUsername());
    }

    @GetMapping(value = "users/{userId}/lines", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LineDTO> getAdminLines(@PathVariable String userId) throws NotFoundException {
        return userService.getAdminLines(userId);
    }

    @PostMapping(value = "users/{userId}/lines", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void addAdminLines(@PathVariable String userId, @RequestBody Set<Long> lines)
            throws BadRequestException, NotFoundException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        userService.addAdminLines(userId, lines, loggedUser.getUsername());
    }

    @DeleteMapping(value="users/{userId}/lines/{lineId}")
    public void removeAdminLine(@PathVariable String userId, @PathVariable Long lineId)
            throws BadRequestException, NotFoundException, ForbiddenException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        userService.removeAdminLine(userId, lineId, loggedUser.getUsername());
    }

    @GetMapping(value = "/users/{userId}/pupils", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PupilDTO> getPupils(@PathVariable String userId)
            throws NotFoundException, ForbiddenException, BadRequestException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userService.getPupils(userId, loggedUser.getUsername());
    }

    @GetMapping(value = "/users/{userId}/rides", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RideDTO> getRides(@PathVariable String userId, @RequestParam Long lineId)
            throws NotFoundException, ForbiddenException, BadRequestException {
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userService.getRides(userId, lineId, loggedUser.getUsername());
    }

//
//    @GetMapping(value = "/register/{userID}", produces = MediaType.APPLICATION_JSON_VALUE)
//    public Map<String,String> getUser(@PathVariable String userID) throws NotFoundException {
//        Map<String,String> returnedBody = new HashMap<>();
//        returnedBody.put("email",userService.getUser(userID));
//        return returnedBody;
//    }

}
