package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingTopicMapper;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingTopicServiceImpl implements BookingTopicService {

    private final BookingTopicMapper bookingTopicMapper;

    @Override
    public List<String> listActiveTopicNames() {
        return bookingTopicMapper.listActiveTopicNames();
    }
}
