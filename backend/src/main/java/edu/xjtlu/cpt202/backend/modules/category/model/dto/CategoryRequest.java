package edu.xjtlu.cpt202.backend.modules.category.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Category name cannot be empty")
    @Size(max = 50, message = "Category name must be at most 50 characters")
    @Pattern(
            regexp = "^[A-Za-z ]+$",
            message = "Category name can only contain letters and spaces"
    )
    private String categoryName;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
