package it.polito.ai.pedibusbackend.viewmodels;

import it.polito.ai.pedibusbackend.entities.User;
import lombok.Data;

@Data
public class NotificationDTO {
    private Long id;
    private String message;
    private Boolean read;
    private Boolean hasNext;
}
