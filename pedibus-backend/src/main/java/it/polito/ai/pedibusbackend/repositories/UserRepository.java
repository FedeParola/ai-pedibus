package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<User, String> {
    //User getByEmail(String email);
}
