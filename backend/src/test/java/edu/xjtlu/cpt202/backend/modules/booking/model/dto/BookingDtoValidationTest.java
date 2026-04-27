package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void bookingCreateDto_rejectsMissingRequiredFields() {
        BookingCreateDTO request = new BookingCreateDTO();
        request.setTopic(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("specialistId is required"));
        assertTrue(messages.contains("slotId is required"));
        assertTrue(messages.contains("topic is required"));
    }

    @Test
    void bookingCreateDto_rejectsOverlongTopicAndUnsupportedNotesCharacters() {
        BookingCreateDTO request = new BookingCreateDTO();
        request.setSpecialistId(1L);
        request.setSlotId(2L);
        request.setTopic("T".repeat(101));
        request.setCustomerNotes("Contains unsupported emoji \uD83D\uDE00");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("topic must be at most 100 characters"));
        assertTrue(messages.contains("customerNotes contains unsupported characters"));
    }

    @Test
    void specialistRejectBookingRequestDto_rejectsBlankAndOverlongReason() {
        SpecialistRejectBookingRequestDTO request = new SpecialistRejectBookingRequestDTO();
        request.setRejectionReason("R".repeat(301));

        Set<String> overlongMessages = messagesFor(request);
        request.setRejectionReason(" ");
        Set<String> blankMessages = messagesFor(request);

        assertTrue(overlongMessages.contains("Rejection reason must be at most 300 characters"));
        assertTrue(blankMessages.contains("Rejection reason is required"));
    }

    @Test
    void specialistForceCancelBookingRequestDto_rejectsMissingReleaseSlotAndInvalidReason() {
        SpecialistForceCancelBookingRequestDTO request = new SpecialistForceCancelBookingRequestDTO();
        request.setCancelReason(" ");

        Set<String> messages = messagesFor(request);

        assertTrue(messages.contains("Cancel reason is required"));
        assertTrue(messages.contains("releaseSlot is required"));
    }

    private Set<String> messagesFor(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
