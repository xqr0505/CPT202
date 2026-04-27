package edu.xjtlu.cpt202.backend.modules.category.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void categoryRequest_rejectsBlankName() {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Category name cannot be empty"));
    }

    @Test
    void categoryRequest_rejectsOverlongNameAndUnsupportedCharacters() {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Cardiology123".repeat(5));

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Category name must be at most 50 characters"));
        assertTrue(messages.contains("Category name can only contain letters and spaces"));
    }

    private Set<String> messagesFor(CategoryRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
