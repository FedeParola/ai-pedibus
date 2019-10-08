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
import it.polito.ai.pedibusbackend.viewmodels.UpdateRideDTO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
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

        // Check if the date is > then 18:00 of the previous day
        //create the day before at 18:00
        long millis = newRideDTO.getDate().getTime() - 8 * 60 * 60 * 1000;
        Date dayBeforeRide = new Date(millis);
        //check if the date if after the day before at 18:00
        //current date
        Date currentDate = new Date();
        if(currentDate.after(dayBeforeRide)){
            throw new BadRequestException("Ride creation time expired");
        }

        /* Add new ride to the DB */
        Ride ride = new Ride();
        ride.setDate(new java.sql.Date(newRideDTO.getDate().getTime()));
        ride.setLine(line);
        ride.setDirection(newRideDTO.getDirection());
        ride.setConsolidated(false);

        return rideRepository.save(ride).getId();
    }

    public void consolidateRide(Long rideId, UpdateRideDTO updateRideDTO, UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Boolean proceed = false;

        Ride ride = rideRepository.getById(rideId);
        /* Check if the user is SYSTEM-ADMIN or ADMIN of line with {lineId} */
        AuthorizationManager.authorizeLineAccess(currentUser, ride.getLine().getId());


        // Check if the date is > then 18:00 of the previous day
        //create the day before at 18:00
        long millis = ride.getDate().getTime() - 8 * 60 * 60 * 1000;
        java.sql.Date dayBeforeRide = new java.sql.Date(millis);
        //check if the date if after the day before at 18:00
        //current date
        Date currentDate = new Date();
        if(currentDate.after(dayBeforeRide)){
            throw new BadRequestException("Ride consolidation time expired");
        }


        //take ride availabilities
        List<Availability> rideAvailabilities = ride.getAvailabilities();
        //list of CONFIRMED availabilities
        List<Availability> confirmedAvailabilities = new ArrayList<>();

        // check if i want to consolidate or unconsoliate the ride
        if(updateRideDTO.getConsolidated()){
            //check if already true
            if(ride.getConsolidated()){
                return;
            }

            //check if the ride is covered and CONSOLIDATE the status of availabilities and the ride consolidated value to true

            //save the first stop of the ride
            Stop firstStop = null;
            for(Stop s : ride.getLine().getStops()){
                if(s.getDirection().equals(ride.getDirection()) && s.getOrder()==0)
                    firstStop = s;
            }

            //check if almost one cover all the ride
            for(Availability a : rideAvailabilities){
                //save all confirmed availabilities
                if(a.getStatus().equals("CONFIRMED")){
                    confirmedAvailabilities.add(a);
                    if(a.getStop().equals(firstStop)){
                        proceed = true;
                    }
                }
            }

            if(proceed){
                //update availabilities with status CONFIRMED
                for(Availability a : confirmedAvailabilities){
                    a.setStatus("CONSOLIDATED");
                    availabilityRepository.save(a);
                }
                //update ride consolidated value
                ride.setConsolidated(true);
                rideRepository.save(ride);
            }else{
                throw new BadRequestException("Ride is not completely covered");
            }
        }else{
            //check if already false
            if(!ride.getConsolidated()){
                return;
            }

            //change the status of the availabilities to CONFIRMED and the ride consolidated value to false
            //update availabilities
            for(Availability a : rideAvailabilities){
                if(a.getStatus().equals("CONSOLIDATED")){
                    a.setStatus("CONFIRMED");
                    availabilityRepository.save(a);
                }
            }
            //update ride consolidated value
            ride.setConsolidated(false);
            rideRepository.save(ride);
        }

        return;
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
