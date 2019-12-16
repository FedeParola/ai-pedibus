package it.polito.ai.pedibusbackend.entities;

import com.vividsolutions.jts.geom.Point;
import lombok.Data;

import javax.persistence.*;
import java.sql.Time;

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

    // @Column(nullable = false)
    private Point location;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Line line;
}
