package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegistrationDTO {
    @Size(min = 1)
    @NotNull
    private String name;

    @Size(min = 1)
    @NotNull
    private String surname;

    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).{6,32}$")
    @NotNull
    private String password;

    @NotNull
    private String passwordConf;
}
