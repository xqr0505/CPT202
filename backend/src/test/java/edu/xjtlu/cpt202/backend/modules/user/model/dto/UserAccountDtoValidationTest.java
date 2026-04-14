package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void updateUserProfileDto_rejectsBlankNameInvalidEmailAndPhone() {
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName(" ");
        request.setEmail("not-an-email");
        request.setPhoneNumber("12345");

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("Full name is required."));
        assertTrue(messages.contains("Please enter a valid email address."));
        assertTrue(messages.contains("Please enter a valid phone number."));
    }

    @Test
    void changePasswordDto_rejectsWeakPassword() {
        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("weak");
        request.setConfirmationPassword("weak");

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains(
                "Password must be at least 8 characters and include uppercase, lowercase, and number"
        ));
    }
}
