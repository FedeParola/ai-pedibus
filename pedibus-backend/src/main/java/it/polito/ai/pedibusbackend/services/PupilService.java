package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.Line;
import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.Reservation;
import it.polito.ai.pedibusbackend.entities.User;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.LineRepository;
import it.polito.ai.pedibusbackend.repositories.PupilRepository;
import it.polito.ai.pedibusbackend.repositories.UserRepository;
import it.polito.ai.pedibusbackend.viewmodels.NewPupilDTO;
import it.polito.ai.pedibusbackend.viewmodels.PupilUpdateDTO;
import it.polito.ai.pedibusbackend.viewmodels.ReservationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PupilService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LineRepository lineRepository;
    @Autowired
    private PupilRepository pupilRepository;


    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public Long createPupil(NewPupilDTO newPupil, String loggedUserId) throws BadRequestException, ForbiddenException {
        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(newPupil.getUserId()) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        /* Retrieve user and line */
        User user = userRepository.findById(newPupil.getUserId()).orElseThrow(BadRequestException::new);
        Line line = lineRepository.findById(newPupil.getLineId()).orElseThrow(BadRequestException::new);

        /* Check duplicate pupil name */
        if (user.getPupils().stream().map(Pupil::getName).collect(Collectors.toList()).contains(newPupil.getName())) {
            throw new BadRequestException();
        }

        /* Persist pupil */
        Pupil pupil = new Pupil();
        pupil.setName(newPupil.getName());
        pupil.setUser(user);
        pupil.setLine(line);
        pupil = pupilRepository.save(pupil);

        return pupil.getId();
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void updatePupil(Long pupilId, PupilUpdateDTO pupilUpdate, String loggedUserId)
            throws BadRequestException, ForbiddenException, NotFoundException {
        Pupil pupil = pupilRepository.findById(pupilId).orElseThrow(NotFoundException::new);

        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(pupil.getUser().getEmail()) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        /* Update only received fields */
        if (pupilUpdate.getName() != null) {
            /* Check duplicate pupil name */
            if (pupil.getUser().getPupils().stream().map(Pupil::getName).collect(Collectors.toList()).contains(pupilUpdate.getName())) {
                throw new BadRequestException();
            }
            pupil.setName(pupilUpdate.getName());
        }

        if (pupilUpdate.getLineId() != null) {
            Line line = lineRepository.findById(pupilUpdate.getLineId()).orElseThrow(BadRequestException::new);
            pupil.setLine(line);
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void deletePupil(Long pupilId, String loggedUserId)
            throws NotFoundException, BadRequestException, ForbiddenException {
        Pupil pupil = pupilRepository.findById(pupilId).orElseThrow(NotFoundException::new);

        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(pupil.getUser().getEmail()) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        pupilRepository.delete(pupil);
    }

    public List<ReservationDTO> getReservations(Long pupilId, String loggedUserId)
            throws ForbiddenException, BadRequestException, NotFoundException {
        Pupil pupil = pupilRepository.findById(pupilId).orElseThrow(NotFoundException::new);

        /* Authorize access */
        User loggedUser = userRepository.findById(loggedUserId).orElseThrow(BadRequestException::new);
        if (!(loggedUserId.equals(pupil.getUser().getEmail()) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        /* Build result structure */
        List<ReservationDTO> reservationDTOs = new ArrayList<>();
        for (Reservation r : pupil.getReservations()) {
            ReservationDTO reservationDTO = new ReservationDTO();
            reservationDTO.setId(r.getId());
            reservationDTO.setRideId(r.getRide().getId());
            reservationDTO.setStopId(r.getStop().getId());
            if (r.getAttendance() != null) {
                reservationDTO.setAttendanceId(r.getAttendance().getId());
            }
            reservationDTOs.add(reservationDTO);
        }

        return reservationDTOs;
    }
}
