package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.ConfirmationToken;
import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ConfirmationTokenRepository extends CrudRepository<ConfirmationToken, Long> {
    Optional<ConfirmationToken> findByUser(User user);
    Optional<ConfirmationToken> findByUuid(String uuid);
}
