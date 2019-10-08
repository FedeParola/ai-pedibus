package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateRideDTO {
    @NotNull
    private Boolean consolidated;
}
