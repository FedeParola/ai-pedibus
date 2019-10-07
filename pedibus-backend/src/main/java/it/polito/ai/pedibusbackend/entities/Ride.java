package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;
import java.util.List;

@Entity
@Data
public class Ride {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Date date;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Line line;

    @Column(nullable = false)
    private Character direction;

    @Column(nullable = false)
    private Boolean consolidated;

    @OneToMany(mappedBy = "ride")
    private List<Reservation> reservations;

    @OneToMany(mappedBy = "ride")
    private List<Attendance> attendances;

    @OneToMany(mappedBy = "ride")
    private List<Availability> availabilities;
}
