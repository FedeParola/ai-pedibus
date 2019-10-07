package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Availability;
import org.springframework.data.repository.CrudRepository;

public interface AvailabilityRepository extends CrudRepository<Availability, Long> {
}
