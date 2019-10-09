package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

@Data
public class ReservationDTO {
    private Long id;
    private Long rideId;
    private Long stopId;
    private Long attendanceId;
}