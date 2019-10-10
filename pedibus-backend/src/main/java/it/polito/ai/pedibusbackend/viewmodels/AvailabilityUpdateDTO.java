package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Min;

@Data
public class AvailabilityUpdateDTO {
    @Min(0)
    private Long stopId;

    private String status; // NEW -> ASSIGNED (admin), ASSIGNED -> CONFIRMED (user),
                           // ASSIGNED -> NEW (admin), CONFIRMED -> NEW (admin),  (CONFIRMED -> CONSOLIDATED)
}
