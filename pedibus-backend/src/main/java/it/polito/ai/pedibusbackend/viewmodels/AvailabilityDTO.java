package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

@Data
public class AvailabilityDTO {
    private Long id;
    private Long rideId;
    private Long stopId;
    private String status;
}
