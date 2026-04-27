package edu.xjtlu.cpt202.backend.modules.auth.model.dto;

import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RefreshTokenRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.ResetPasswordRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.VerifyResetCodeRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void loginRequest_rejectsBlankEmailPasswordAndRole() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" ");
        request.setPassword(" ");
        request.setRole(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Email is required"));
        assertTrue(messages.contains("Password is required"));
        assertTrue(messages.contains("Role is required"));
    }

    @Test
    void registerRequest_rejectsMissingRequiredFields() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(" ");
        request.setVerificationCode(" ");
        request.setPassword(" ");
        request.setConfirmPassword(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Email is required"));
        assertTrue(messages.contains("Verification code is required"));
        assertTrue(messages.contains("Password is required"));
        assertTrue(messages.contains("Confirm password is required"));
    }

    @Test
    void sendVerificationCodeRequest_rejectsInvalidEmailAndBlankType() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("not-an-email");
        request.setType(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Invalid email format"));
        assertTrue(messages.contains("Type is required"));
    }

    @Test
    void resetPasswordRequest_rejectsInvalidEmailAndBlankPasswords() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("not-an-email");
        request.setVerificationCode(" ");
        request.setNewPassword(" ");
        request.setConfirmPassword(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Invalid email format"));
        assertTrue(messages.contains("Verification code is required"));
        assertTrue(messages.contains("New password is required"));
        assertTrue(messages.contains("Confirm password is required"));
    }

    @Test
    void resetCodeDtos_rejectInvalidOrBlankEmailAndCode() {
        SendResetCodeRequest sendRequest = new SendResetCodeRequest();
        sendRequest.setEmail("not-an-email");
        VerifyResetCodeRequest verifyRequest = new VerifyResetCodeRequest();
        verifyRequest.setEmail(" ");
        verifyRequest.setVerificationCode(" ");

        Set<String> sendMessages = messagesFor(sendRequest);
        Set<String> verifyMessages = messagesFor(verifyRequest);

        assertFalse(sendMessages.isEmpty());
        assertTrue(verifyMessages.contains("Email is required"));
        assertTrue(verifyMessages.contains("Verification code is required"));
    }

    @Test
    void refreshTokenRequest_rejectsBlankRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Refresh token is required"));
    }

    private Set<String> messagesFor(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
