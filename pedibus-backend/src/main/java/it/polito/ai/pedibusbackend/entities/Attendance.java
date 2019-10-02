package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Data
public class Attendance {
    @Id
    @GeneratedValue
    @Column(name = "Id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PupilId", nullable = false)
    private Pupil pupil;

    @OneToOne
    @JoinColumn(name = "ReservationId", nullable = true)
    private Reservation reservation;

    @Column(name = "Date", nullable = false)
    private Date date;

    @Column(name = "Direction", nullable = false)
    private Character direction;
}
