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
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @Column(nullable = false)
    private Character direction;

    @Column(nullable = false)
    private Time time;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Line line;
}
