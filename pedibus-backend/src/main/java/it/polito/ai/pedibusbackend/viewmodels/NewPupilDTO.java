package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class NewPupilDTO {
    @Size(min = 1)
    @NotNull
    private String name;
    @NotNull
    private String userId;
    @NotNull
    private Long lineId;
}
