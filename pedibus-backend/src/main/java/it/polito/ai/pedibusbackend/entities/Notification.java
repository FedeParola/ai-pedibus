package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
public class Notification {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Boolean read;

    @Column(nullable = false)
    private Timestamp timestamp;
}
