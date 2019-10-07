package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.AvailabilityRepository;
import it.polito.ai.pedibusbackend.repositories.LineRepository;
import it.polito.ai.pedibusbackend.repositories.RideRepository;
import it.polito.ai.pedibusbackend.repositories.UserRepository;
import it.polito.ai.pedibusbackend.security.AuthorizationManager;
import it.polito.ai.pedibusbackend.viewmodels.NewRideDTO;
import it.polito.ai.pedibusbackend.viewmodels.RideDTO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RideService implements InitializingBean {
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LineRepository lineRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;

    public List<RideDTO> getRides() {
        List<RideDTO> rides = new ArrayList<>();
        RideDTO rideDTO;

        for (Ride r: rideRepository.findAll()) {
            rideDTO = new RideDTO();
            rideDTO.setId(r.getId());
            rideDTO.setDate(r.getDate());
            rideDTO.setDirection(r.getDirection());
            rideDTO.setConsolidated(r.getConsolidated());
            rides.add(rideDTO);
        }

        return rides;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Long addRide(NewRideDTO newRideDTO, UserDetails loggedUser) throws BadRequestException, NotFoundException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        /* Retrieve the line from lineId */
        Line line = lineRepository.getById(newRideDTO.getLineId());
        if(line == null) {
            throw new NotFoundException("Line with id " + newRideDTO.getLineId() + " not found");
        }

        /* Check if the user is SYSTEM-ADMIN or ADMIN of line with {lineId} */
        AuthorizationManager.authorizeLineAccess(currentUser, line.getId());

        /* Add new ride to the DB */
        Ride ride = new Ride();
        ride.setDate(new java.sql.Date(newRideDTO.getDate().getTime()));
        ride.setLine(line);
        ride.setDirection(newRideDTO.getDirection());
        ride.setConsolidated(false);

        return rideRepository.save(ride).getId();
    }

    public void deleteRide(Long rideId, UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Ride ride = rideRepository.getById(rideId);

        /* Check if the user is SYSTEM-ADMIN or ADMIN of line with {lineId} */
        AuthorizationManager.authorizeLineAccess(currentUser, ride.getLine().getId());

        rideRepository.delete(ride);
        return;
    }

    public List<Reservation> getRideReservations(Long rideId, UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Ride ride = rideRepository.getById(rideId);

        /* Check if the user is SYSTEM-ADMIN or ADMIN of line with {lineId} */
        AuthorizationManager.authorizeLineAccess(currentUser, ride.getLine().getId());

        /* Check if the currentUser has availability status 'CONSOLIDATED' */
        Availability a = availabilityRepository.getByUserAndRide(currentUser, ride);
        if(!a.getStatus().equals("CONSOLIDATED")){
            throw new ForbiddenException();
        }

        return ride.getReservations();
    }

    public List<Attendance> getRideAttendances(Long rideId, UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Ride ride = rideRepository.getById(rideId);

        /* Check if the user is SYSTEM-ADMIN or ADMIN of line with {lineId} */
        AuthorizationManager.authorizeLineAccess(currentUser, ride.getLine().getId());

        /* Check if the currentUser has availability status 'CONSOLIDATED' */
        Availability a = availabilityRepository.getByUserAndRide(currentUser, ride);
        if(!a.getStatus().equals("CONSOLIDATED")){
            throw new ForbiddenException();
        }

        return ride.getAttendances();
    }

    public List<Availability> getRideAvailabilities(Long rideId, UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Ride ride = rideRepository.getById(rideId);

        /* Check if the user is SYSTEM-ADMIN or ADMIN of line with {lineId} */
        AuthorizationManager.authorizeLineAccess(currentUser, ride.getLine().getId());

        return ride.getAvailabilities();
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
