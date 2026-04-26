package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void updateUserProfileDto_rejectsInvalidEmailAndOverlongFields() {
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName("A".repeat(61));
        request.setEmail("not-an-email");
        request.setPhoneNumber("+86 " + "1".repeat(15));

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("Full name must be at most 60 characters."));
        assertTrue(messages.contains("Please enter a valid email address."));
        assertTrue(messages.contains("Phone number must be at most 18 characters."));
    }

    @Test
    void updateUserProfileDto_allowsBlankOptionalFields() {
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setFullName(" ");
        request.setEmail(" ");
        request.setPhoneNumber(" ");

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertFalse(messages.contains("Full name is required."));
        assertFalse(messages.contains("Please enter a valid phone number."));
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

    @Test
    void confirmCurrentPasswordDto_rejectsBlankPassword() {
        ConfirmCurrentPasswordDTO request = new ConfirmCurrentPasswordDTO();
        request.setCurrentPassword(" ");

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("Current password is required"));
    }

    @Test
    void sendChangeEmailCodeDto_rejectsBlankOrInvalidEmail() {
        SendChangeEmailCodeDTO request = new SendChangeEmailCodeDTO();
        request.setNewEmail("not-an-email");

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("Please enter a valid email address."));
    }

    @Test
    void changeCurrentUserEmailDto_rejectsInvalidEmailAndCode() {
        ChangeCurrentUserEmailDTO request = new ChangeCurrentUserEmailDTO();
        request.setNewEmail(" ");
        request.setCode("12ab");

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("New email is required."));
        assertTrue(messages.contains("Verification code must be 6 digits."));
    }
}
