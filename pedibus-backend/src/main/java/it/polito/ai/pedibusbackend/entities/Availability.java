package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Availability {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Ride ride;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Stop stop;

    @Column(nullable = false)
    private String status; // NEW, ASSIGNED, CONFIRMED, CONSOLIDATED
}
