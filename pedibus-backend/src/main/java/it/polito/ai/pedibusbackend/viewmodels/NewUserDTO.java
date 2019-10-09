package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class NewUserDTO {
    @Email
    @Size(min = 1)
    @NotNull
    String email;
}
