package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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
