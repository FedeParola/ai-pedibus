package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.*;
import it.polito.ai.pedibusbackend.viewmodels.NewReservationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Date;

@Service
@DependsOn("userService")
public class ReservationService{
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PupilRepository pupilRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private SimpMessagingTemplate msgTemplate;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Long addReservation(@Valid NewReservationDTO reservationDTO, UserDetails loggedUser) throws BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        //Check existence of the referenced entities
        Ride ride = rideRepository.findById(reservationDTO.getRideId()).orElse(null);
        if(ride == null) {
            throw new BadRequestException("Unknown ride");
        }

        Pupil pupil = pupilRepository.findById(reservationDTO.getPupilId()).orElse(null);
        if(pupil == null) {
            throw new BadRequestException("Unknown pupil");
        }

        Stop stop = stopRepository.findById(reservationDTO.getStopId()).orElse(null);
        if(stop == null) {
            throw new BadRequestException("Unknown stop");
        }

        //Check if the logged user is the system admin
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            //Check if the pupil belongs to the logged user
            if(!currentUser.getEmail().equals(pupil.getUser().getEmail())){
                throw new ForbiddenException("The user is not allowed to do this action");
            }
        }

        //Check if current date and time is before than when the ride takes place
        Date utilDate = new Date();
        if(ride.getDate().compareTo(new java.sql.Date(utilDate.getTime())) < 0){
            throw new BadRequestException("The ride has already taken place");
        }

        //Check if the stop belongs to the ride
        if(!ride.getLine().getStops().stream()
                                     .filter((s) -> s.getId() == stop.getId())
                                     .findAny().isPresent()  ||  !ride.getDirection().equals(stop.getDirection())){
            throw new BadRequestException("The stop does not belong to the ride");
        }

        //Check if the pupil is already reserved somewhere the same day in the same direction
        Reservation r = reservationRepository.findByPupilAndDateAndDirection(pupil, ride.getDate(),
                ride.getDirection()).orElse(null);
        if(r != null){
            throw new BadRequestException("The pupil is already reserved on another ride at the same time");
        }

        //Create the reservation and add it to the repository
        Reservation reservation = new Reservation();
        reservation.setPupil(pupil);
        reservation.setRide(ride);
        reservation.setStop(stop);
        reservation = reservationRepository.save(reservation);

        //Warn each escort of the ride
        for (Availability a : availabilityRepository.findByRideAndStatus(ride, "CONSOLIDATED")){
            if(!a.getUser().getEmail().equals(currentUser.getEmail())) {
                String direction = ride.getDirection().equals("O") ? "outbound" : "return";
                notificationService.createNotification(a.getUser(), "New reservation", "New reservation for " +
                        pupil.getName() + " of user '" + pupil.getUser().getEmail() + "' on stop '" + stop.getName() +
                        "' for the " + direction + " direction of line '" + ride.getLine().getName() + "' on " + ride.getDate());
            }
        }
        // Notify reservation creation
        notifyReservationOperation(reservation);

        return reservation.getId();
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void updateReservation(Long reservationId, Long newStopId, UserDetails loggedUser) throws NotFoundException,
            BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if(reservation == null) {
            throw new NotFoundException("Reservation not found");
        }

        //Check if the user can update the reservation (user who made it or system admin)
        //AuthorizationManager.authorizeReservationAccess(currentUser, reservation);
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!currentUser.getEmail().equals(reservation.getPupil().getUser().getEmail())){
                throw new ForbiddenException("The user is not allowed to do this action");
            }
        }

        //Check stop existence
        Stop stop = stopRepository.findById(newStopId).orElse(null);
        if(stop == null) {
            throw new BadRequestException("Unknown stop");
        }

        //Check that the new stop belongs to the same ride
        if(!reservation.getRide().getLine().getStops().stream()
                                                      .filter((s) -> s.getId() == stop.getId())
                                                      .findAny().isPresent()  ||  !reservation.getRide().getDirection().equals(stop.getDirection())){
            throw new BadRequestException("The new stop does not belong to the same ride");
        }

        reservation.setStop(stop);

        reservationRepository.save(reservation);

        //Warn each escort of the ride
        for (Availability a : availabilityRepository.findByRideAndStatus(reservation.getRide(), "CONSOLIDATED")) {
            if(!a.getUser().getEmail().equals(currentUser.getEmail())) {
                String direction = reservation.getRide().getDirection().equals("O") ? "outbound" : "return";
                notificationService.createNotification(a.getUser(), "Reservation updated", "Reservation for " +
                        reservation.getPupil().getName() + " of user '" + currentUser.getEmail() + "' for the " + direction +
                        " direction of line '" + reservation.getRide().getLine().getName() + "' on " + reservation.getRide().getDate() +
                        " has been updated");
            }
        }

        // Notify reservation update
        notifyReservationOperation(reservation);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void deleteReservation(Long reservationId, UserDetails loggedUser) throws NotFoundException, BadRequestException,
            ForbiddenException {
        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if(reservation == null) {
            throw new NotFoundException("Reservation not found");
        }

        //Check if the user can delete the reservation (user who made it or system admin)
        //AuthorizationManager.authorizeReservationAccess(currentUser, reservation);
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!currentUser.getEmail().equals(reservation.getPupil().getUser().getEmail())){
                throw new ForbiddenException("The user is not allowed to do this action");
            }
        }

        reservationRepository.delete(reservation);

        //Warn each escort of the ride but the one who deleted the reservation (if it is an escort of this ride)
        for (Availability a : availabilityRepository.findByRideAndStatus(reservation.getRide(), "CONSOLIDATED")) {
            if(!a.getUser().equals(currentUser)){
                String direction = reservation.getRide().getDirection().equals("O") ? "outbound" : "return";
                notificationService.createNotification(a.getUser(), "A reservation was cancelled", "Reservation for " +
                        reservation.getPupil().getName() + " of user '" + currentUser.getEmail() +
                        "' on stop '" + reservation.getStop().getName() + "' for the " + direction + " direction of line '" +
                        reservation.getRide().getLine().getName() + "' on " + reservation.getRide().getDate() + " has been cancelled");
            }
        }

        // Notify reservation deletion
        notifyReservationOperation(reservation);
    }

    private void notifyReservationOperation(Reservation reservation) {
        msgTemplate.convertAndSend("/topic/rides/" + reservation.getRide().getId() + "/reservations", "");
        msgTemplate.convertAndSend("/topic/pupils/" + reservation.getPupil().getId() + "/reservations", "");
    }

}
