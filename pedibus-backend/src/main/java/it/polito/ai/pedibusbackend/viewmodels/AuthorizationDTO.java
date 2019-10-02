package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AuthorizationDTO {

    @NotNull
    private String action;
    @NotNull
    private String lineName;
}
