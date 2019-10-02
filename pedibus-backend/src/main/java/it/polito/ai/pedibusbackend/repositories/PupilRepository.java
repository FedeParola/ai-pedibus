package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Pupil;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PupilRepository extends CrudRepository<Pupil, Long> {
}
