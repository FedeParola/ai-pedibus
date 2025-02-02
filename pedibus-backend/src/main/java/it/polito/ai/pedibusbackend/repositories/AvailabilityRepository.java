package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Availability;
import it.polito.ai.pedibusbackend.entities.Line;
import it.polito.ai.pedibusbackend.entities.Ride;
import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends CrudRepository<Availability, Long> {
    Availability getByUserAndRide(User user, Ride ride);

    @Query("SELECT a FROM Availability a JOIN a.ride r ON r.line = ?2 " +
            "WHERE a.user = ?1 AND a.status = ?3")
    Iterable<Availability> findByUserAndLineAndStatus(User user, Line line, String status);

    @Query("SELECT a FROM Availability a JOIN a.ride r ON r.line = ?2 " +
            "WHERE a.user = ?1")
    Iterable<Availability> findByUserAndLine(User user, Line line);

    @Query("SELECT a FROM Availability " +
            "a JOIN a.ride r ON r.date = ?2 AND r.direction = ?3 " +
            "WHERE a.user = ?1")
    Optional<Availability> findByUserAndDateAndDirection(User user, Date date, Character direction);

    List<Availability> findByRideAndStatus(Ride ride, String consolidated);
}
