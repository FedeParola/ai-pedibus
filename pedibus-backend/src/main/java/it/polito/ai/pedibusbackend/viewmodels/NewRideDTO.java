package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class NewRideDTO {
    @NotNull
    private Date date;
    @NotNull
    private Long lineId;
    @NotNull
    private Character direction;
}
