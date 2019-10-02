package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.Reservation;
import it.polito.ai.pedibusbackend.entities.Stop;
import org.springframework.data.repository.CrudRepository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends CrudRepository<Reservation, Long> {
    List<Reservation> getByStopAndDate(Stop stop, Date date);
    List<Reservation> getByPupilAndDate(Pupil pupil, Date date);
}
