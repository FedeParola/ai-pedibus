package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;
import java.sql.Time;
import java.util.List;

@Entity
@Data
public class Stop {
    @Id
    @GeneratedValue
    @Column(name = "Id")
    private Long id;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "Position", nullable = false)
    private Integer position;

    @Column(name = "Direction", nullable = false)
    private Character direction;

    @Column(name = "Time", nullable = false)
    private Time time;

    @ManyToOne
    @JoinColumn(name = "LineId", nullable = false)
    private Line line;

    @OneToMany(mappedBy = "stop")
    private List<Reservation> reservations;
}
