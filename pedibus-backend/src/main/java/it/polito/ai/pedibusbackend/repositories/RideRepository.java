package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Line;
import it.polito.ai.pedibusbackend.entities.Ride;
import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends CrudRepository<Ride, Long> {
    Optional<Ride> getById(Long rideId);
    List<Ride> getByLine(Line line);

    @Query("SELECT r FROM Ride r JOIN r.availabilities a ON a.user = ?1 AND " +
           "a.status = 'CONSOLIDATED' " +
           "WHERE r.line = ?2")
    Iterable<Ride> getByEscortAndLine(User escort, Line line);

    Optional<Ride> getByLineAndDateAndDirection(Line line, Date date, Character direction);
}
