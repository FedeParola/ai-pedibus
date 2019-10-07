package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.Attendance;
import it.polito.ai.pedibusbackend.entities.Line;
import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.Reservation;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.AttendanceRepository;
import it.polito.ai.pedibusbackend.repositories.LineRepository;
import it.polito.ai.pedibusbackend.repositories.PupilRepository;
import it.polito.ai.pedibusbackend.repositories.ReservationRepository;
import it.polito.ai.pedibusbackend.viewmodels.AttendanceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AttendanceService {
//    @Autowired
//    private AttendanceRepository attendanceRepository;
//    @Autowired
//    private LineRepository lineRepository;
//    @Autowired
//    private PupilRepository pupilRepository;
//    @Autowired
//    private ReservationRepository reservationRepository;
//
//    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
//    public Long addAttendance(AttendanceDTO attendanceDTO, String lineName, Date date) throws NotFoundException, BadRequestException {
//        Line line = lineRepository.getByName(lineName);
//
//        /*Check line and pupil existence*/
//        if(line == null) {
//            throw new NotFoundException("Line " + lineName + " not found");
//        }
//
//        Pupil pupil = pupilRepository.findById(attendanceDTO.getPupilId())
//                .orElseThrow(() -> new NotFoundException("Pupil " + attendanceDTO.getPupilId() + " not found"));
//
//        /*Check if the pupil is already present somewhere the same day in the same direction*/
//        Attendance a = attendanceRepository.getByPupilAndDateAndDirection(pupil, new java.sql.Date(date.getTime()), attendanceDTO.getDirection().charAt(0))
//                .orElse(null);
//        if(a != null){
//            throw new BadRequestException("The pupil was already marked as present");
//        }
//
//        /*Get the (eventually) linked reservation*/
//        List<Reservation> reservations = reservationRepository.getByPupilAndDate(pupil, new java.sql.Date(date.getTime()));
//        Reservation foundRes = null;
//        for(Reservation r : reservations){
//            if(r.getStop().getDirection().equals(attendanceDTO.getDirection().charAt(0))){
//                if(!r.getStop().getLine().getName().equals(lineName)){
//                    throw new BadRequestException("The attendance does not match the reservation");
//                }
//                foundRes = r;
//            }
//        }
//
//        /*Create the attendance and add it to the repo*/
//        Attendance attendance = new Attendance();
//        attendance.setPupil(pupil);
//        attendance.setDate(new java.sql.Date(date.getTime()));
//        attendance.setDirection(attendanceDTO.getDirection().charAt(0));
//        if(foundRes != null){
//            attendance.setReservation(foundRes);
//        }
//
//        attendance = attendanceRepository.save(attendance);
//
//        return attendance.getId();
//    }
//
//    public void deleteAttendance(Long attendanceId, String lineName, Date date) throws NotFoundException {
//        Attendance attendance = getAttendanceFromUri(attendanceId, lineName, date);
//        attendanceRepository.delete(attendance);
//        return;
//    }
//
//    private Attendance getAttendanceFromUri(Long attendanceId, String lineName, Date date) throws NotFoundException {
//        Attendance attendance = attendanceRepository.findById(attendanceId)
//                .orElseThrow(() -> new NotFoundException("Attendance with id " + attendanceId + " not found"));
//
//        String attendanceLine;
//        if(attendance.getReservation() != null){
//            attendanceLine = attendance.getReservation().getStop().getLine().getName();
//        }
//        else{
//            attendanceLine = attendance.getPupil().getLine().getName();
//        }
//
//        if(!attendance.getDate().equals(date)  ||  !attendanceLine.equals(lineName)) {
//            throw new NotFoundException("Attendance with id " + attendanceId + " not found");
//        }
//
//        return attendance;
//    }
}
