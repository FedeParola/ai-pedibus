package it.polito.ai.pedibusbackend.controllers;

import it.polito.ai.pedibusbackend.exceptions.BadRequestException;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;
import it.polito.ai.pedibusbackend.exceptions.NotFoundException;
import it.polito.ai.pedibusbackend.services.NotificationService;
import it.polito.ai.pedibusbackend.viewmodels.NotificationUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @RequestMapping(value = "/notifications/{notificationId}", method = RequestMethod.DELETE)
    public void deleteNotification(@PathVariable Long notificationId, Principal principal) throws NotFoundException,
            BadRequestException, ForbiddenException {
        notificationService.deleteNotification(notificationId, principal.getName());
    }

    @PutMapping(value = "/notifications/{notificationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updatePupil(@PathVariable Long notificationId, @RequestBody NotificationUpdateDTO notificationUpdate, Principal principal)
            throws BadRequestException, NotFoundException, ForbiddenException {
        notificationService.updateNotification(notificationId, notificationUpdate, principal.getName());
    }
}
