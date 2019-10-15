package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityDTO {
    private Long id;
    private RideDTO ride;
    private Long stopId;
    private String status;
    private String userId;
}
