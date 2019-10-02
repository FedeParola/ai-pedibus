package it.polito.ai.pedibusbackend.entities;

import lombok.Data;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
public class Line {
    @Id
    @GeneratedValue
    @Column(name = "Id")
    private Long id;

    @Column(name = "Name", nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "line")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Stop> stops;

    @OneToMany(mappedBy = "line")
    private List<Pupil> pupils;
}
