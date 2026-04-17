package edu.xjtlu.cpt202.backend.modules.specialist.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminSpecialistStatusUpdateDTO {

    @NotBlank(message = "status cannot be empty")
    @Pattern(
            regexp = "^(Active|Inactive)$",
            message = "status must be Active or Inactive"
    )
    private String status;
}
