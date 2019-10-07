package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Availability;
import it.polito.ai.pedibusbackend.entities.Ride;
import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AvailabilityRepository extends CrudRepository<Availability, Long> {
    Availability getByUserAndRide(User user, Ride ride);
}
