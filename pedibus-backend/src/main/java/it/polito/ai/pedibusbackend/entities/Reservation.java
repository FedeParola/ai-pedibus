package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Data
public class Reservation {
    @Id
    @GeneratedValue
    @Column(name = "Id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PupilId", nullable = false)
    private Pupil pupil;

    @Column(name = "Date", nullable = false)
    private Date date;

    @ManyToOne
    @JoinColumn(name = "StopId", nullable = false)
    private Stop stop;

    @OneToOne(mappedBy = "reservation")
    private Attendance attendance;
}
