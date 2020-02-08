package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.Availability;
import it.polito.ai.pedibusbackend.entities.Ride;
import it.polito.ai.pedibusbackend.entities.Stop;
import it.polito.ai.pedibusbackend.entities.User;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.AvailabilityRepository;
import it.polito.ai.pedibusbackend.repositories.RideRepository;
import it.polito.ai.pedibusbackend.repositories.StopRepository;
import it.polito.ai.pedibusbackend.repositories.UserRepository;
import it.polito.ai.pedibusbackend.security.AuthorizationManager;
import it.polito.ai.pedibusbackend.viewmodels.AvailabilityUpdateDTO;
import it.polito.ai.pedibusbackend.viewmodels.NewAvailabilityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Date;

@Service
public class AvailabilityService {
    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private SimpMessagingTemplate msgTemplate;
    @Autowired
    private NotificationService notificationService;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public Long addAvailability(@Valid NewAvailabilityDTO availabilityDTO, UserDetails loggedUser)
            throws BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        //Check existence of the referenced entities
        User user = userRepository.findById(availabilityDTO.getEmail()).orElse(null);
        if(user == null) {
            throw new BadRequestException("Unknown user");
        }

        Ride ride = rideRepository.findById(availabilityDTO.getRideId()).orElse(null);
        if(ride == null) {
            throw new BadRequestException("Unknown ride");
        }

        Stop stop = stopRepository.findById(availabilityDTO.getStopId()).orElse(null);
        if(stop == null) {
            throw new BadRequestException("Unknown stop");
        }

        //Check if the ride is already consolidated
        if(ride.getConsolidated()){
            throw new BadRequestException("The ride is already consolidated");
        }

        //Check if the stop belongs to the ride
        if(!ride.getLine().getStops().stream()
                                     .filter((s) -> s.getId() == stop.getId())
                                     .findAny().isPresent()  ||  !stop.getDirection().equals(ride.getDirection())){
            throw new BadRequestException("The stop does not belong to the ride");
        }

        //Check if current date and time is before the deadline (18:00 of the previous day)
        //(giusto?)
        long millis = ride.getDate().getTime() - 6 * 60 * 60 * 1000;
        Date dayBeforeRide = new Date(millis);
        Date currentDate = new Date();
        if(currentDate.after(dayBeforeRide)){
            throw new BadRequestException("Ride creation time expired");
        }

        //Check if the user can provide availability (sys admin for anyone, or any user only for himself)
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!availabilityDTO.getEmail().equals(currentUser.getEmail())){
                throw new ForbiddenException("The user is not allowed to do this action!");
            }
        }

        //Create the availability and add it to the repository
        Availability availability = new Availability();
        availability.setUser(user);
        availability.setRide(ride);
        availability.setStop(stop);
        availability.setStatus("NEW");
        availability = availabilityRepository.save(availability);

        //Warn each admin of the line of the ride
        for(User u : ride.getLine().getUsers()){
            if(!u.getEmail().equals(currentUser.getEmail())) {
                String direction = ride.getDirection().equals("O") ? "outbound" : "return";
                notificationService.createNotification(u, "New availability", "User '" + user.getEmail() +
                        "' is available for ride of line '" + ride.getLine().getName() + "' " + "for the " + direction +
                        " direction " + "on " + ride.getDate() + " starting from the stop '" + stop.getName() + "'");
            }
        }

        // Notify availability creation
        msgTemplate.convertAndSend(
                "/topic/users/" + user.getEmail() + "/availabilities?lineId=" + ride.getLine().getId(),
                "");
        msgTemplate.convertAndSend("/topic/rides/"+ride.getId()+"/availabilities", "Availability created");

        return availability.getId();
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void updateAvailability(Long availabilityId, AvailabilityUpdateDTO newAvailability, UserDetails loggedUser)
            throws NotFoundException, BadRequestException, ForbiddenException {
        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());
        Boolean updated = false;

        Availability availability = availabilityRepository.findById(availabilityId).orElse(null);
        if(availability == null) {
            throw new NotFoundException("Availability not found");
        }

        //Check if the ride is already consolidated
        if(availability.getRide().getConsolidated()){
            throw new BadRequestException("The ride is already consolidated");
        }

        //Check if current date and time is before the deadline (18:00 of the previous day)
        // (giusto?)
        long millis = availability.getRide().getDate().getTime() - 6 * 60 * 60 * 1000;
        Date dayBeforeRide = new Date(millis);
        Date currentDate = new Date();
        if(currentDate.after(dayBeforeRide)){
            throw new BadRequestException("Ride creation time expired");
        }

        //Get the new field(s) of the availability
        Long newStopId = newAvailability.getStopId();
        String newStatus = newAvailability.getStatus();

        //Update the stop from(on outward)/to(on return) which the user is available
        if(newStopId != null){
            if (availability.getStatus().equals("CONFIRMED")){
                throw new BadRequestException("Can not update the stop of a confirmed availability");
            }

            Stop newStop = stopRepository.findById(newStopId).orElse(null);
            if(newStop == null){
                throw new BadRequestException("Unknown stop");
            }

            //Check if the stop belongs to the ride
            if(!availability.getRide().getLine().getStops().stream()
                                                           .filter((s) -> s.getId() == newStop.getId())
                                                           .findAny().isPresent()  ||  !availability.getRide().getDirection().equals(newStop.getDirection())){
                throw new BadRequestException("The stop does not belong to the ride");
            }

            //Check if the user can update the availability's stop (sys admin for anyone, or any user only for himself)
            if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
                if(!availability.getUser().getEmail().equals(currentUser.getEmail())){
                    throw new ForbiddenException("The user is not allowed to do this action");
                }
            }

            availability.setStop(newStop);
            updated = true;
        }

        //Update the status
        if(newStatus != null){
            String oldStatus = availability.getStatus();
            //Check if the new status is a valid one (and if the transition is a valid one)
            if(newStatus.equals("NEW")){
                if(oldStatus.equals("ASSIGNED")){
                    //only admin of that line or sys admin
                    AuthorizationManager.authorizeLineAccess(currentUser, availability.getRide().getLine());

                    Ride ride = availability.getRide();
                    //Warn the escort who provided the availability for the ride
                    if(!availability.getUser().getEmail().equals(currentUser.getEmail())) {
                        String direction = ride.getDirection().equals("O") ? "outbound" : "return";
                        notificationService.createNotification(availability.getUser(), "Ride no longer assigned",
                                "Your assignment to ride of line '" + ride.getLine().getName() + "' " +
                                        "for the " + direction + " direction " + "on " + ride.getDate() + " has been cancelled");
                    }
                } else if (oldStatus.equals("CONFIRMED")) {
                    //only admin of that line or sys admin
                    AuthorizationManager.authorizeLineAccess(currentUser, availability.getRide().getLine());
                } else {
                    throw new BadRequestException("This transition of status of the availability is not a valid one");
                }
            } else if(newStatus.equals("ASSIGNED")) {
                if(oldStatus.equals("NEW")){
                    //only admin of that line or sys admin
                    AuthorizationManager.authorizeLineAccess(currentUser, availability.getRide().getLine());

                    Ride ride = availability.getRide();
                    //Warn the escort who provided the availability for the ride
                    if(!availability.getUser().getEmail().equals(currentUser.getEmail())) {
                        String direction = ride.getDirection().equals("O") ? "outbound" : "return";
                        notificationService.createNotification(availability.getUser(), "Ride assigned", "You have been " +
                                "assigned for the " + direction + " direction of line '" + ride.getLine().getName() + "' " +
                                "on " + ride.getDate());
                    }
                } else {
                    throw new BadRequestException("This transition of status of the availability is not a valid one");
                }
            } else if(newStatus.equals("CONFIRMED")){
                if(oldStatus.equals("ASSIGNED")){
                    //only user who provided the availability or sys admin
                    if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
                        if(!currentUser.getEmail().equals(availability.getUser().getEmail())){
                            throw new ForbiddenException("The user is not allowed to do this action");
                        }
                    }

                    //Check if the user has already confirmed his availability somewhere the same day in the same direction
                    Availability a = availabilityRepository.findByUserAndDateAndDirection(availability.getUser(),
                            availability.getRide().getDate(), availability.getRide().getDirection()).orElse(null);
                    if(a != null  &&  a.getStatus().equals("CONFIRMED")){
                        throw new BadRequestException("The user has already confirmed his availability for a ride " +
                                "at the same time");
                    }

                    Ride ride = availability.getRide();
                    //Warn each admin of the line of the ride
                    for(User u : ride.getLine().getUsers()){
                        if(!u.getEmail().equals(currentUser.getEmail())) {
                            String direction = ride.getDirection().equals("O") ? "outbound" : "return";
                            notificationService.createNotification(u, "Availability confirmed", "User '" + availability.getUser().getEmail() +
                                    "' has confirmed his availability for ride of line '" + ride.getLine().getName() + "' " +
                                    "for the " + direction + " direction " + "on " + ride.getDate());
                        }
                    }
                } else {
                    throw new BadRequestException("This transition of status of the availability is not a valid one");
                }
            } else {
                throw new BadRequestException("Invalid status");
            }

            availability.setStatus(newStatus);
            updated = true;
        }

        availabilityRepository.save(availability);

        if(updated){
            // Notify availability update
            msgTemplate.convertAndSend(
                    "/topic/users/" + availability.getUser().getEmail() + "/availabilities?lineId=" +
                            availability.getRide().getLine().getId(),
                    "");
            msgTemplate.convertAndSend("/topic/rides/"+availability.getRide().getId()+"/availabilities",
                    "Availability updated");
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void deleteAvailability(Long availabilityId, UserDetails loggedUser) throws BadRequestException,
            NotFoundException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Availability availability = availabilityRepository.findById(availabilityId).orElse(null);
        if(availability == null) {
            throw new NotFoundException("Availability not found");
        }

        //Check if the user can delete the availability (user who provided it or sytem admin)
        //AuthorizationManager.authorizeAvailabilityAccess(currentUser, reservation);
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!availability.getUser().getEmail().equals(currentUser.getEmail())){
                throw new ForbiddenException("The user is not allowed to do this action");
            }
        }

        //Check if availability is already confirmed or consolidated
        if(availability.getStatus().equals("CONFIRMED")  ||  availability.getStatus().equals("CONSOLIDATED")){
            throw new BadRequestException("The availability is already confirmed or consolidate, and thus cannot be deleted");
        }

        availabilityRepository.delete(availability);

        Ride ride = availability.getRide();
        //Warn each admin of the line of the ride
        for(User u : ride.getLine().getUsers()){
            if(!u.getEmail().equals(currentUser.getEmail())) {
                String direction = ride.getDirection().equals("O") ? "outbound" : "return";
                notificationService.createNotification(u, "Availability cancelled", "User '" + availability.getUser().getEmail() +
                        "' is no longer available for ride of line '" + ride.getLine().getName() + "' " +
                        "for the " + direction + " direction " + "on " + ride.getDate());
            }
        }

        // Notify availability deletion
        msgTemplate.convertAndSend(
                "/topic/users/" + availability.getUser().getEmail() + "/availabilities?lineId=" +
                        availability.getRide().getLine().getId(),
                "");
        msgTemplate.convertAndSend("/topic/rides/"+availability.getRide().getId()+"/availabilities",
                "Availability deleted");
    }
}
