package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDTO {
    @NotNull
    @Min(0)
    private Long pupilId;

    @NotNull
    @Pattern(regexp = "O|R")
    private String direction;

    private Long id;
    private Long stopId;
    private Long reservationId;
    private Boolean hasReservation;
}
