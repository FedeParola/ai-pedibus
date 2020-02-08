package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Line;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LineRepository extends CrudRepository<Line, Long> {
    Line getById(Long id);
    List<Line> findAll();
}
