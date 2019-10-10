package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class NewAttendanceDTO {
    @NotNull
    @Min(0)
    private Long pupilId;
    @NotNull
    @Min(0)
    private Long rideId;
    @NotNull
    @Min(0)
    private Long stopId;
}
