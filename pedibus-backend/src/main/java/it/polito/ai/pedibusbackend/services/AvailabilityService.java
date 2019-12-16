package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Date;

@Service
public class AvailabilityService {
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private StopRepository stopRepository;


    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Long addAvailability(@Valid NewAvailabilityDTO availabilityDTO, UserDetails loggedUser)
            throws BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        //Check existence of the referenced entities
        User user = userRepository.findById(availabilityDTO.getEmail()).orElse(null);
        if(user == null) {
            throw new BadRequestException("Unknown user with id " + availabilityDTO.getEmail());
        }

        Ride ride = rideRepository.findById(availabilityDTO.getRideId()).orElse(null);
        if(ride == null) {
            throw new BadRequestException("Unknown ride with id " + availabilityDTO.getRideId());
        }

        Stop stop = stopRepository.findById(availabilityDTO.getStopId()).orElse(null);
        if(stop == null) {
            throw new BadRequestException("Unknown stop with id " + availabilityDTO.getStopId());
        }

        //Check if the ride is already consolidated
        if(ride.getConsolidated()){
            throw new BadRequestException("The ride is already consolidated!");
        }

        //Check if the stop belongs to the ride
        if(!ride.getLine().getStops().contains(stop)  ||  !stop.getDirection().equals(ride.getDirection())) {
            throw new BadRequestException("The stop with id " + stop.getId() + " does not belong to the ride with id " + ride.getId());
        }

        //Check if current date and time is before the deadline (18:00 of the previous day)
        //(giusto?)
        long millis = ride.getDate().getTime() - 8 * 60 * 60 * 1000;
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

        return availability.getId();
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void updateAvailability(Long availabilityId, AvailabilityUpdateDTO newAvailability, UserDetails loggedUser)
            throws NotFoundException, BadRequestException, ForbiddenException {
        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Availability availability = availabilityRepository.findById(availabilityId).orElse(null);
        if(availability == null) {
            throw new NotFoundException("Availability with id " + availabilityId + " not found");
        }

        //Check if the ride is already consolidated
        if(availability.getRide().getConsolidated()){
            throw new BadRequestException("The ride is already consolidated");
        }

        //Check if current date and time is before the deadline (18:00 of the previous day)
        long millis = availability.getRide().getDate().getTime() - 8 * 60 * 60 * 1000;
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
            Stop newStop = stopRepository.findById(newStopId).orElse(null);
            if(newStop == null){
                throw new BadRequestException("Unknown stop with id " + newStopId);
            }

            //Check if the stop belongs to the same ride of the old availability
            if(!availability.getRide().getLine().getStops().contains(newStop)  ||  !availability.getRide().getDirection().
                    equals(newStop.getDirection())){
                throw new BadRequestException("The new stop does not belong to the same ride of the availability");
            }

            //Check if the user can update the availability's stop (sys admin for anyone, or any user only for himself)
            if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
                if(!availability.getUser().getEmail().equals(currentUser.getEmail())){
                    throw new ForbiddenException("The user is not allowed to do this action");
                }
            }

            availability.setStop(newStop);
        }

        //Update the status
        if(newStatus != null){
            String oldStatus = availability.getStatus();
            //Check if the new status is a valid one (and if the transition is a valid one)
            if(newStatus.equals("NEW")){
                if(oldStatus.equals("ASSIGNED")){
                    //only admin of that line or sys admin
                    AuthorizationManager.authorizeLineAccess(currentUser, availability.getRide().getLine());
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
                        throw new BadRequestException("The user has already confirmed his availability somewhere " +
                                "the same day in the same direction");
                    }
                } else {
                    throw new BadRequestException("This transition of status of the availability is not a valid one");
                }
            } else {
                throw new BadRequestException();
            }

            availability.setStatus(newStatus);
        }

        availabilityRepository.save(availability);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void deleteAvailability(Long availabilityId, UserDetails loggedUser) throws BadRequestException,
            NotFoundException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Availability availability = availabilityRepository.findById(availabilityId).orElse(null);
        if(availability == null) {
            throw new NotFoundException("Availability with id " + availabilityId + " not found");
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
    }
}
