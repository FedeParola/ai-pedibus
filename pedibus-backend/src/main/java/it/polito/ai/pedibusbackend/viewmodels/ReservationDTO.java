package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationDTO {
    private Long id;
    private PupilDTO pupil;
    private Long rideId;
    private Long stopId;
    private Long attendanceId;
    private Boolean hasAttendance;
}