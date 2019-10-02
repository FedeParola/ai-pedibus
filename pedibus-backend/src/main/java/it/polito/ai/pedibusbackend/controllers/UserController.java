package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.security.jwt.JwtTokenProvider;
import it.polito.ai.pedibusbackend.services.MailService;
import it.polito.ai.pedibusbackend.services.UserService;
import it.polito.ai.pedibusbackend.viewmodels.AuthorizationDTO;
import it.polito.ai.pedibusbackend.viewmodels.LoginDTO;
import it.polito.ai.pedibusbackend.viewmodels.RecoverDTO;
import it.polito.ai.pedibusbackend.viewmodels.RegistrationDTO;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void register(@RequestBody @Valid RegistrationDTO registrationDTO, HttpServletRequest request) throws BadRequestException {
        /* Check password confirmation */
        if (!registrationDTO.getPass().equals(registrationDTO.getConfPass())) {
            throw new BadRequestException("Password confirmation doesn't match");
        }

        String uuid = userService.registerUser(registrationDTO);

        /* Build the confirmation URL */
        String requestUrl = request.getRequestURL().toString();
        String confirmUrl = requestUrl.substring(0, requestUrl.lastIndexOf(request.getRequestURI())) + "/confirm/" + uuid;

        mailService.sendConfirmationMail(registrationDTO.getEmail(), confirmUrl);

        return;
    }

    @GetMapping(value = "/confirm/{randomUUID}")
    public ModelAndView confirm(@PathVariable String randomUUID) throws NotFoundException {
        userService.confirmUser(randomUUID);

        ModelAndView response = new ModelAndView();
        response.setViewName("accountConfirmed");

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
    public List<String> getUsers(@RequestParam("page") Optional<Integer> page, @RequestParam("size") Optional<Integer> size)
            throws BadRequestException {
        return userService.getAllUsers(page, size);
    }

    @PutMapping(value = "/users/{userID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void modifyUserRole(@PathVariable String userID,@RequestBody @Valid AuthorizationDTO authorizationDTO)
            throws BadRequestException, ForbiddenException {
        //retrieve username of currently logged user
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(authorizationDTO.getAction().equals("GRANT")){
            userService.grantUser(userID, authorizationDTO, loggedUser);
        }else if(authorizationDTO.getAction().equals("REVOKE")){
            userService.revokeUser(userID,authorizationDTO,loggedUser);
        }else{
            throw new BadRequestException();
        }

        return;
    }

    @GetMapping(value = "/register/{userID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> getUser(@PathVariable String userID) throws NotFoundException {
        Map<String,String> returnedBody = new HashMap<>();
        returnedBody.put("email",userService.getUser(userID));
        return returnedBody;
    }

}
