package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDTO {
    private Long id;
    private PupilDTO pupil;
    private Long rideId;
    private Long stopId;
    private Boolean hasReservation;
}
