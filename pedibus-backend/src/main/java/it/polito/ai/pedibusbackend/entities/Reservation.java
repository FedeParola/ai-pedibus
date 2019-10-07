package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Data
public class Reservation {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Pupil pupil;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Ride ride;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Stop stop;

    @OneToOne(mappedBy = "reservation")
    private Attendance attendance;
}
