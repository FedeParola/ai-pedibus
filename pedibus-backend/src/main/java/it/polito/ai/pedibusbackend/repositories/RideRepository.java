package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Ride;
import org.springframework.data.repository.CrudRepository;

public interface RideRepository extends CrudRepository<Ride, Long> {
}
