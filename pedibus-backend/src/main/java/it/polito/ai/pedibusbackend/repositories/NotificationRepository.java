package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Notification;
import it.polito.ai.pedibusbackend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface NotificationRepository extends PagingAndSortingRepository<Notification, Long> {
    Page<Notification> findByUser(User user, Pageable pageable);
    Long countByUserAndRead(User user, Boolean read);
    Iterable<Object> findByUser(User user);
}
