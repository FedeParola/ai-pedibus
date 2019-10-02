package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineDTO {
    @NotNull
    private String name;
    @Valid
    private List<StopDTO> outwardStops;
    @Valid
    private List<StopDTO> returnStops;
}
