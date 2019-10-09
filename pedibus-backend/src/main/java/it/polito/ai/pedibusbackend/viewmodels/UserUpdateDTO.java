package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserUpdateDTO {
    @Size(min = 1)
    private String name;
    @Size(min = 1)
    private String surname;
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).{6,32}$")
    private String password;
}
