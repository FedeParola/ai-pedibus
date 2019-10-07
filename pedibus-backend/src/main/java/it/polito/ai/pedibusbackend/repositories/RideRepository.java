package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Line;
import it.polito.ai.pedibusbackend.entities.Ride;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RideRepository extends CrudRepository<Ride, Long> {
    Ride getById(Long rideId);
    List<Ride> getByLine(Line line);
}
