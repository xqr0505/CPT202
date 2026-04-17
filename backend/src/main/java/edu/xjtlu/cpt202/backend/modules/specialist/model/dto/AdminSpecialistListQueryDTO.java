package edu.xjtlu.cpt202.backend.modules.specialist.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminSpecialistListQueryDTO {

    @Size(max = 100, message = "keyword must be at most 100 characters")
    private String keyword;

    private Long categoryId;

    @Pattern(
            regexp = "^(Active|Inactive)$",
            message = "status must be Active or Inactive"
    )
    private String status;

    @Min(value = 1, message = "pageNo must be at least 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be at least 1")
    @Max(value = 100, message = "pageSize must not exceed 100")
    private Integer pageSize = 10;
}
