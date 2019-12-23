package it.polito.ai.pedibusbackend.entities;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="\"user\"")
@Data
public class User {
    @Id
    private String email;

    private String name;

    private String surname;

    private String password;

    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>();

    @ManyToMany()
    private List<Line> lines = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Pupil> pupils = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications = new ArrayList<>();
}
