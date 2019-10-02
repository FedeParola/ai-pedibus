package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegistrationDTO {
    @Email
    @Size(min = 1)
    @NotNull
    private String email;
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).{6,32}$", message = "The password must contain at least " +
            "one uppercase and one lowercase character and a digit and be at least 6 characters long")
    @NotNull
    private String pass;
    @NotNull
    private String confPass;
}
