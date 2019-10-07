package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class StopDTO {
    private Long id;
    @NotNull
    private String name;
    @NotNull
    @Min(0)
    private Integer order;
    @NotNull
    @Pattern(regexp = "(([01][0-9])|(2[0-4])):[0-5][0-9]")
    private String time;
}
