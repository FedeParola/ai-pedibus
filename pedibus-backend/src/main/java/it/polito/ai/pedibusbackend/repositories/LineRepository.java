package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Line;
import org.springframework.data.repository.CrudRepository;

public interface LineRepository extends CrudRepository<Line, Long> {
    Line getById(Long id);
}
