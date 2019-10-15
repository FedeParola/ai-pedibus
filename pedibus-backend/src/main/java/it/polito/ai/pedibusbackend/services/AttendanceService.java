package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.*;
import it.polito.ai.pedibusbackend.viewmodels.NewAttendanceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PupilRepository pupilRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Long addAttendance(@Valid NewAttendanceDTO attendanceDTO, UserDetails loggedUser) throws BadRequestException, ForbiddenException {
        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        //Check existence of the referenced entities
        Ride ride = rideRepository.findById(attendanceDTO.getRideId()).orElse(null);
        if(ride == null) {
            throw new BadRequestException("Unknown ride with id " + attendanceDTO.getRideId());
        }

        Pupil pupil = pupilRepository.findById(attendanceDTO.getPupilId()).orElse(null);
        if(pupil == null) {
            throw new BadRequestException("Unknown pupil with id " + attendanceDTO.getPupilId());
        }

        Stop stop = stopRepository.findById(attendanceDTO.getStopId()).orElse(null);
        if(stop == null) {
            throw new BadRequestException("Unknown stop with id " + attendanceDTO.getStopId());
        }

        //Check if the user can add the attendance (system admin or 'accompagnatore' of that ride)
        //AuthorizationManager.authorizeAttendanceAccess(currentUser, attendance);
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!ride.getConsolidated()) {
                throw new ForbiddenException();
            } else {
                Availability a = availabilityRepository.getByUserAndRide(currentUser, ride);
                if(a == null) {
                    throw new ForbiddenException();
                }
//                if(ride.getDirection().equals("O")  &&  stop.getOrder() < a.getStop().getOrder()){
//                    throw new ForbiddenException();
//                }
//                if(ride.getDirection().equals("R")  &&  stop.getOrder() > a.getStop().getOrder()){
//                    throw new ForbiddenException();
//                }
            }
        }

        //Check if current date and time is before than when the stop takes place (with 30 min tolerance)
        Date stopTime = new Date(ride.getDate().getTime() + stop.getTime().getTime());
        Date now = new Date();
        now.setTime(now.getTime() - 30 * 60 * 1000);
        if(stopTime.compareTo(now) <  0){
            throw new BadRequestException();
        }

        //Check if the stop belongs to the ride
        if(!ride.getLine().getStops().stream().anyMatch(s -> s.getId() == stop.getId())){
            throw new BadRequestException();
        }

        //Check if the pupil is already present somewhere the same day in the same direction
        Attendance a = attendanceRepository.findByPupilAndDateAndDirection(pupil, ride.getDate(),
                ride.getDirection()).orElse(null);
        if(a != null){
            throw new BadRequestException("The pupil was already marked as present");
        }

        //Create the attendance and add it to the repository
        Attendance attendance = new Attendance();
        attendance.setPupil(pupil);
        attendance.setRide(ride);
        attendance.setStop(stop);

        //Get the (potentially) linked reservation
        Reservation r = reservationRepository.findByPupilAndRideAndStop(pupil, ride, stop).orElse(null);
        if(r != null){
            attendance.setReservation(r);
        }

        attendance = attendanceRepository.save(attendance);

        return attendance.getId();
    }

    public void deleteAttendance(Long attendanceId, UserDetails loggedUser) throws NotFoundException, BadRequestException,
            ForbiddenException {
        User currentUser = userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());

        Attendance attendance = attendanceRepository.findById(attendanceId).orElse(null);
        if(attendance == null){
            throw new NotFoundException("Attendance with id " + attendanceId + " not found");
        }

        //Check if current date and time is before than when the stop takes place (with 30 min tolerance)
        Date stopTime = new Date(attendance.getRide().getDate().getTime() + attendance.getStop().getTime().getTime());
        Date now = new Date();
        now.setTime(now.getTime() - 30 * 60 * 1000);
        if(stopTime.compareTo(now) <  0){
            throw new BadRequestException();
        }

        //Check if the user can add the attendance (system admin or 'accompagnatore' of that ride)
        //AuthorizationManager.authorizeAttendanceAccess(currentUser, attendance);
        if(!currentUser.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!attendance.getRide().getConsolidated()) {
                throw new ForbiddenException();
            } else {
                Availability a = availabilityRepository.getByUserAndRide(currentUser, attendance.getRide());
                if(a == null){
                    throw new ForbiddenException();
                }
//                if(ride.getDirection().equals("O")  &&  stop.getOrder() < a.getStop().getOrder()){
//                    throw new ForbiddenException();
//                }
//                if(ride.getDirection().equals("R")  &&  stop.getOrder() > a.getStop().getOrder()){
//                    throw new ForbiddenException();
//                }
            }
        }

        attendanceRepository.delete(attendance);
    }
}
