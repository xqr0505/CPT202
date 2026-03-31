package edu.xjtlu.cpt202.backend.modules.schedule.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class SpecialistSearchQueryDTO {

    private String keyword;

    private Long categoryId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String sortBy = "recommended";

    @Min(value = 1, message = "pageNo must be at least 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be at least 1")
    @Max(value = 24, message = "pageSize must not exceed 24")
    private Integer pageSize = 12;
}
