package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface PupilRepository extends PagingAndSortingRepository<Pupil, Long> {
    Page<Pupil> findByUser(User user, Pageable pageable);
}
