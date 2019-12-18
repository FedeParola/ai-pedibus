package it.polito.ai.pedibusbackend.viewmodels;

import it.polito.ai.pedibusbackend.entities.User;
import lombok.Data;

import java.util.Date;

@Data
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private Boolean read;
    private Boolean hasNext;
    private Date date;
}
