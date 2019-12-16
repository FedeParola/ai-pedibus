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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Date;

@Service
@DependsOn("userService")
public class ReservationService implements InitializingBean {
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

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Long addReservation(@Valid NewReservationDTO reservationDTO, UserDetails loggedUser) throws BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        //Check existence of the referenced entities
        Ride ride = rideRepository.findById(reservationDTO.getRideId()).orElse(null);
        if(ride == null) {
            throw new BadRequestException("Unknown ride with id " + reservationDTO.getRideId());
        }

        Pupil pupil = pupilRepository.findById(reservationDTO.getPupilId()).orElse(null);
        if(pupil == null) {
            throw new BadRequestException("Unknown pupil with id " + reservationDTO.getPupilId());
        }

        Stop stop = stopRepository.findById(reservationDTO.getStopId()).orElse(null);
        if(stop == null) {
            throw new BadRequestException("Unknown stop with id " + reservationDTO.getStopId());
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
            throw new BadRequestException("The pupil was already reserved that day in the same direction");
        }

        //Create the reservation and add it to the repository
        Reservation reservation = new Reservation();
        reservation.setPupil(pupil);
        reservation.setRide(ride);
        reservation.setStop(stop);
        reservation = reservationRepository.save(reservation);

        return reservation.getId();
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void updateReservation(Long reservationId, Long newStopId, UserDetails loggedUser) throws NotFoundException,
            BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if(reservation == null) {
            throw new NotFoundException("Reservation with id " + reservationId + " not found");
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
            throw new BadRequestException("Unknown stop with id " + newStopId);
        }

        //Check that the new stop belongs to the same ride
        if(!reservation.getRide().getLine().getStops().stream()
                                                      .filter((s) -> s.getId() == stop.getId())
                                                      .findAny().isPresent()  ||  !reservation.getRide().getDirection().equals(stop.getDirection())){
            throw new BadRequestException("The new stop does not belong to the same ride");
        }

        reservation.setStop(stop);
        reservationRepository.save(reservation);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void deleteReservation(Long reservationId, UserDetails loggedUser) throws NotFoundException, BadRequestException,
            ForbiddenException {
        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if(reservation == null) {
            throw new NotFoundException("Reservation with id " + reservationId + " not found");
        }

        //Check if the user can delete the reservation (user who made it or system admin)
        //AuthorizationManager.authorizeReservationAccess(currentUser, reservation);
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!currentUser.getEmail().equals(reservation.getPupil().getUser().getEmail())){
                throw new ForbiddenException("The user is not allowed to do this action");
            }
        }

        reservationRepository.delete(reservation);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
//        log.info("Inizializzazione!");
//
//        /* Create some random reservations for yesterday, today and tomorrow */
//        Random rnd = new Random(); // Adding a seed we can replicate the same reservations!
//
//        /* Scan all pupils */
//        for (Pupil p: pupilRepository.findAll()) {
//            /* Split stops by direction */
//            List<Stop> outStops = new ArrayList<>();
//            List<Stop> retStops = new ArrayList<>();
//            p.getLine().getStops().forEach((s) -> {
//                if (s.getDirection().equals('O')) {
//                    outStops.add(s);
//                } else {
//                    retStops.add(s);
//                }
//            });
//
//            /* Scan days */
//            long millis = System.currentTimeMillis() - 24 * 60 * 60 * 1000; // Yesterday
//            for (int i = 0; i < 3; i++) {
//                java.sql.Date date = new java.sql.Date(millis);
//
//                /* Random choose for outward reservation */
//                if (rnd.nextDouble() > 0.25) {
//                    Reservation r = new Reservation();
//                    r.setDate(date);
//                    r.setPupil(p);
//                    r.setStop(outStops.get(rnd.nextInt(outStops.size()))); // Pick a random stop
//                    reservationRepository.save(r);
//                }
//
//                /* Random choose for backward reservation */
//                if (rnd.nextDouble() > 0.25) {
//                    Reservation r = new Reservation();
//                    r.setDate(date);
//                    r.setPupil(p);
//                    r.setStop(retStops.get(rnd.nextInt(retStops.size()))); // Pick a random stop
//                    reservationRepository.save(r);
//                }
//
//                /* Go to next day */
//                millis += 24 * 60 * 60 * 1000;
//            }
//        }
    }
}
