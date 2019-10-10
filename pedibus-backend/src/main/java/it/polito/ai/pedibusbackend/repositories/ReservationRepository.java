package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.Reservation;
import it.polito.ai.pedibusbackend.entities.Ride;
import it.polito.ai.pedibusbackend.entities.Stop;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.sql.Date;
import java.util.Optional;

public interface ReservationRepository extends CrudRepository<Reservation, Long> {
    Optional<Reservation> findByPupilAndRideAndStop(Pupil pupil, Ride ride, Stop stop);

    @Query("SELECT r FROM Reservation " +
            "r JOIN r.ride ride ON ride.date = ?2 AND ride.direction = ?3" +
            "WHERE r.pupil = ?1")
    Optional<Reservation> findByPupilAndDateAndDirection(Pupil pupil, Date date, Character direction);
}
