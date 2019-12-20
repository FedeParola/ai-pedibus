package it.polito.ai.pedibusbackend.services;

import it.polito.ai.pedibusbackend.entities.Notification;
import it.polito.ai.pedibusbackend.entities.Pupil;
import it.polito.ai.pedibusbackend.entities.User;
import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.repositories.NotificationRepository;
import it.polito.ai.pedibusbackend.repositories.UserRepository;
import it.polito.ai.pedibusbackend.viewmodels.NotificationUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SimpMessagingTemplate template;

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

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void updateNotification(Long notificationId, NotificationUpdateDTO notificationUpdate, String name)
            throws BadRequestException, ForbiddenException, NotFoundException {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(NotFoundException::new);

        /* Authorize access */
        User loggedUser = userRepository.findById(name).orElseThrow(BadRequestException::new);
        if (!(name.equals(notification.getUser().getEmail()) || loggedUser.getRoles().contains("ROLE_SYSTEM-ADMIN"))) {
            throw new ForbiddenException();
        }

        /* Update only received fields */
        if (notificationUpdate.getRead() != null) {
            notification.setRead(notificationUpdate.getRead());
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void createNotification(User user, String title, String body) {
        //Create new notification
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(body);
        Date now = new Date();
        notification.setTimestamp(new Timestamp(now.getTime()));
        notification.setRead(false);
        //Persist it
        notificationRepository.save(notification);
        //Warn the user about the new notification
        template.convertAndSendToUser(user.getEmail(), "/topic/notifications", "New notification!");
    }
}
