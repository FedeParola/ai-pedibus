package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class ReservationDTO {
    private Long id;
    @NotNull
    @Min(0)
    private Long pupilId;
    @NotNull
    @Min(0)
    private Long stopId;
    @NotNull
    @Pattern(regexp = "O|R")
    private String direction;
}