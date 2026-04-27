package edu.xjtlu.cpt202.backend.modules.specialist.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSpecialistDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createDto_rejectsBlankInvalidAndMissingFields() {
        AdminSpecialistCreateDTO request = new AdminSpecialistCreateDTO();
        request.setName(" ");
        request.setEmail("not-an-email");
        request.setLevel(" ");
        request.setConsultationFee(new BigDecimal("-1.00"));
        request.setStatus("Paused");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("name cannot be empty"));
        assertTrue(messages.contains("email format is invalid"));
        assertTrue(messages.contains("categoryId is required"));
        assertTrue(messages.contains("level cannot be empty"));
        assertTrue(messages.contains("consultationFee must be greater than or equal to 0"));
        assertTrue(messages.contains("status must be Active or Inactive"));
    }

    @Test
    void updateDto_rejectsShortPasswordAndInvalidStatus() {
        AdminSpecialistUpdateDTO request = validUpdateRequest();
        request.setPassword("short");
        request.setStatus("Disabled");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("password length must be between 8 and 64 characters"));
        assertTrue(messages.contains("status must be Active or Inactive"));
    }

    @Test
    void statusUpdateDto_rejectsBlankOrUnsupportedStatus() {
        AdminSpecialistStatusUpdateDTO request = new AdminSpecialistStatusUpdateDTO();
        request.setStatus(" ");
        Set<String> blankMessages = messagesFor(request);

        request.setStatus("Suspended");
        Set<String> invalidMessages = messagesFor(request);

        assertTrue(blankMessages.contains("status cannot be empty"));
        assertTrue(invalidMessages.contains("status must be Active or Inactive"));
    }

    @Test
    void listQueryDto_rejectsInvalidPaginationAndStatus() {
        AdminSpecialistListQueryDTO request = new AdminSpecialistListQueryDTO();
        request.setKeyword("K".repeat(101));
        request.setStatus("Archived");
        request.setPageNo(0);
        request.setPageSize(101);

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("keyword must be at most 100 characters"));
        assertTrue(messages.contains("status must be Active or Inactive"));
        assertTrue(messages.contains("pageNo must be at least 1"));
        assertTrue(messages.contains("pageSize must not exceed 100"));
    }

    private AdminSpecialistUpdateDTO validUpdateRequest() {
        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr Alice");
        request.setEmail("alice@example.com");
        request.setCategoryId(1L);
        request.setLevel("Senior");
        request.setConsultationFee(new BigDecimal("100.00"));
        request.setStatus("Active");
        return request;
    }

    private Set<String> messagesFor(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
