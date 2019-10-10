package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PupilDTO {
    private Long id;
    private String name;
    private Long lineId;
}
