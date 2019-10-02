package it.polito.ai.pedibusbackend.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(name = "Email")
    private String email;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "Enabled", nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    private List<Line> lines = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Pupil> pupils;
}
