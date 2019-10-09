package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class PupilUpdateDTO {
    @Size(min = 1)
    private String name;
    private Long lineId;
}
