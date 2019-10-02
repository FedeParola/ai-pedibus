package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.RecoverToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RecoverTokenRepository extends CrudRepository<RecoverToken, Long> {
    Optional<RecoverToken> findByUuid(String uuid);
}
