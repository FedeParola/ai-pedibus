package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class NewAvailabilityDTO {
    @NotNull
    @Email
    private String email;
    @NotNull
    @Min(0)
    private Long rideId;
    @NotNull
    @Min(0)
    private Long stopId;
}
