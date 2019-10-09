package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.RegistrationToken;
import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RegistrationTokenRepository extends CrudRepository<RegistrationToken, Long> {
    Optional<RegistrationToken> findByUser(User user);
    Optional<RegistrationToken> findByUuid(String uuid);
}
