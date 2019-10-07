package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class RideDTO {
    @NotNull
    private Long id;
    @NotNull
    private Date date;
    @NotNull
    private Character direction;
    @NotNull
    private Boolean consolidated;
}
