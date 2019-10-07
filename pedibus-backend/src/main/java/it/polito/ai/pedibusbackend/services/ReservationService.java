package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.*;
import it.polito.ai.pedibusbackend.security.AuthorizationManager;
import it.polito.ai.pedibusbackend.viewmodels.ReservationDTO;
import it.polito.ai.pedibusbackend.viewmodels.ReservationsDTO;
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

import java.util.*;

@Service
@DependsOn("userService")
public class ReservationService implements InitializingBean {
//    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private StopRepository stopRepository;
//    @Autowired
//    private ReservationRepository reservationRepository;
//    @Autowired
//    private LineRepository lineRepository;
//    @Autowired
//    private PupilRepository pupilRepository;
//    @Autowired
//    private AttendanceRepository attendanceRepository;
//
//    public ReservationsDTO getReservations(String lineName, Date date, UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
//        /* Get the requested line */
//        Line line = lineRepository.getByName(lineName);
//        if(line == null) {
//            throw new NotFoundException("Line " + lineName + " not found");
//        }
//        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException()); //get the current user from db
//
//        // AuthorizationManager.authorizeLineAccess(currentUser, line);
//
//        ReservationsDTO reservationsDTO = new ReservationsDTO();
//        Map<Long, Pupil> outNoRes = new HashMap<>();
//        Map<Long, Pupil> retNoRes = new HashMap<>();
//        /*Add all the pupils associated to the line to the lists of pupils not reserved*/
//        for(Pupil p : line.getPupils()){
//            outNoRes.put(p.getId(), p);
//            retNoRes.put(p.getId(), p);
//        }
//
//        for(Stop stop : line.getStops()) {
//            /* Prepare a list of reservations for the current stop */
//            ReservationsDTO.StopReservations stopReservations = new ReservationsDTO.StopReservations();
//            stopReservations.setStopName(stop.getName());
//            stopReservations.setStopTime(stop.getTime().toString().substring(0, 5));
//
//            /* Add reservations for the requested date to the list */
//            for(Reservation reservation : reservationRepository.getByStopAndDate(stop, new java.sql.Date(date.getTime()))) {
//                Attendance attendance = reservation.getAttendance();
//                Long attendanceId;
//                if(attendance == null){
//                    attendanceId = (long)-1;
//                }
//                else{
//                    attendanceId = attendance.getId();
//                }
//                stopReservations.addPupil(reservation.getPupil().getId(), reservation.getPupil().getName(), attendanceId);
//                /*Remove the pupil from the list of pupils not reserved*/
//                if(stop.getDirection() == 'O') {
//                    outNoRes.remove(reservation.getPupil().getId());
//                } else {
//                    retNoRes.remove(reservation.getPupil().getId());
//                }
//            }
//
//            /* Add the reservations to the correct direction */
//            if(stop.getDirection() == 'O') {
//                reservationsDTO.getOutwardReservations().add(stopReservations);
//            } else {
//                reservationsDTO.getReturnReservations().add(stopReservations);
//            }
//        }
//
//        /*Check if each pupil is present or not*/
//        for(Pupil p : outNoRes.values()){
//            Attendance attendance = attendanceRepository.getByPupilAndDateAndDirection(p, new java.sql.Date(date.getTime()), 'O')
//                    .orElse(null);
//            Long attendanceId;
//            if(attendance == null){
//                attendanceId = (long)-1;
//            }
//            else{
//                attendanceId = attendance.getId();
//            }
//            reservationsDTO.getOutwardNoRes().add(pupilEntityToDto(p, attendanceId));
//        }
//        for(Pupil p : retNoRes.values()){
//            Attendance attendance = attendanceRepository.getByPupilAndDateAndDirection(p, new java.sql.Date(date.getTime()), 'R')
//                    .orElse(null);
//            Long attendanceId;
//            if(attendance == null){
//                attendanceId = (long)-1;
//            }
//            else{
//                attendanceId = attendance.getId();
//            }
//            reservationsDTO.getReturnNoRes().add(pupilEntityToDto(p, attendanceId));
//        }
//
//        return reservationsDTO;
//    }
//
//    private ReservationsDTO.Pupil pupilEntityToDto(Pupil pupil, Long attendanceId){
//        return new ReservationsDTO.Pupil(pupil.getId(), pupil.getName(), attendanceId);
//    }
//
//    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
//    public Long addReservation(ReservationDTO reservationDTO, String lineName, Date date, UserDetails loggedUser) throws BadRequestException, NotFoundException {
//        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());
//
//        Line line = lineRepository.getByName(lineName);
//        if(line == null) {
//            throw new NotFoundException("Line " + lineName + " not found");
//        }
//
//        Stop stop = stopRepository.findById(reservationDTO.getStopId()).orElse(null);
//        if(stop == null) {
//            throw new BadRequestException("Unknown stop with id " + reservationDTO.getStopId());
//        }
//
//        Pupil pupil = pupilRepository.findById(reservationDTO.getPupilId()).orElse(null);
//        if(pupil == null) {
//            throw new BadRequestException("Unknown pupil with id " + reservationDTO.getPupilId());
//        }
//
//        if(!line.equals(stop.getLine())) {
//            throw new BadRequestException("Stop with id " + reservationDTO.getStopId() + "doesn't belong to line " + lineName);
//        }
//
//        if(reservationDTO.getDirection().charAt(0) != stop.getDirection().charValue()) {
//            throw new BadRequestException("The requested stop isn't available for the requested direction");
//        }
//
//        Reservation reservation = new Reservation();
//        reservation.setStop(stop);
//        reservation.setDate(new java.sql.Date(date.getTime()));
//        reservation.setPupil(pupil);
//
//        reservation = reservationRepository.save(reservation);
//
//        return reservation.getId();
//    }
//
//    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
//    public void updateReservation(String lineName, Date date, Long reservationId, ReservationDTO reservationDTO,
//                                  UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
//        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());
//        Reservation reservation = getReservationFromUri(lineName, date, reservationId);
//        AuthorizationManager.authorizeReservationAccess(currentUser, reservation);
//
//        /* Update the stop */
//        Stop stop = stopRepository.findById(reservationDTO.getStopId()).orElse(null);
//        if(!reservationDTO.getStopId().equals(reservation.getStop().getId())) {
//            if(stop == null) {
//                throw new BadRequestException("Unknown stop with id " + reservationDTO.getStopId());
//            }
//
//            if(!stop.getLine().getName().equals(lineName)) {
//                throw new BadRequestException("Stop with id " + reservationDTO.getStopId() + "doesn't belong to line " + lineName);
//            }
//
//            if(reservationDTO.getDirection().charAt(0) != stop.getDirection().charValue()) {
//                throw new BadRequestException("The requested stop isn't available for the requested direction");
//            }
//
//            reservation.setStop(stop);
//        }
//
//        /*Redundant control to prevent an user from modifying the direction without modifying the stopId*/
//        if(reservationDTO.getDirection().charAt(0) != stop.getDirection().charValue()) {
//            throw new BadRequestException("The requested stop isn't available for the requested direction");
//        }
//
//        reservationRepository.save(reservation);
//
//        return;
//    }
//
//    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
//    public void deleteReservation(String lineName, Date date, Long reservationId,
//                                  UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
//        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());
//        Reservation reservation = getReservationFromUri(lineName, date, reservationId);
//        AuthorizationManager.authorizeReservationAccess(currentUser, reservation);
//
//        reservationRepository.delete(reservation);
//
//        return;
//    }
//
//    public ReservationDTO getReservation(String lineName, Date date, Long reservationId,
//                                         UserDetails loggedUser) throws NotFoundException, BadRequestException, ForbiddenException {
//        User currentUser=userRepository.findById(loggedUser.getUsername()).orElseThrow(() -> new BadRequestException());
//        Reservation reservation = getReservationFromUri(lineName, date, reservationId);
//        AuthorizationManager.authorizeReservationAccess(currentUser, reservation);
//
//        ReservationDTO reservationDTO = new ReservationDTO();
//        reservationDTO.setId(reservation.getId());
//        reservationDTO.setDirection(reservation.getStop().getDirection().toString());
//        reservationDTO.setStopId(reservation.getStop().getId());
//        reservationDTO.setPupilId(reservation.getPupil().getId());
//
//        return reservationDTO;
//    }
//
//    private Reservation getReservationFromUri(String lineName, Date date, Long reservationId) throws NotFoundException {
//        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
//
//        if(reservation == null) {
//            throw new NotFoundException("Reservation with id " + reservationId + " not found");
//        }
//
//        if(!date.equals(reservation.getDate()) || !reservation.getStop().getLine().getName().equals(lineName)) {
//            throw new NotFoundException("Reservation with id " + reservationId + " not found");
//        }
//
//        return reservation;
//    }
//
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
