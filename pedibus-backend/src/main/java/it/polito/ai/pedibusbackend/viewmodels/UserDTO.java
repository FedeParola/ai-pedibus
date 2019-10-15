package it.polito.ai.pedibusbackend.viewmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Value;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserDTO {
    private String email;
    private String name;
    private String surname;
    private boolean enabled;
    private List<String> roles;
    private List<LineDTO> lines;
}
