package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/booking-topics")
@RequiredArgsConstructor
public class BookingTopicController {

    private final BookingTopicService bookingTopicService;

    @GetMapping
    public Result<List<String>> listBookingTopics() {
        return Result.success(bookingTopicService.listActiveTopicNames());
    }
}
