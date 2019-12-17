package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.Notification;
import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.User;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.NotificationRepository;
import it.polito.ai.pedibusbackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    public void deleteNotification(Long notificationId, String name) throws NotFoundException, BadRequestException,
            ForbiddenException {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(NotFoundException::new);

        /* Authorize access */
        User loggedUser = userRepository.findById(name).orElseThrow(BadRequestException::new);
        if (!(name.equals(notification.getUser().getEmail()) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        notificationRepository.delete(notification);
    }
}
