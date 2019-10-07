package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Reservation;
import org.springframework.data.repository.CrudRepository;

public interface ReservationRepository extends CrudRepository<Reservation, Long> {
}
