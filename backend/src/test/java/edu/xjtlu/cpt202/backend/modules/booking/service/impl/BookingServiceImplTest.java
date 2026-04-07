package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author QiranXiao
 * @date 2026/4/1
 *
 */
@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @Mock
    private SpecialistQueryService specialistQueryService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    public void getUpcomingBookingsByCustomer_Success() {
        // Arrange
        Long customerId = 1L;
        int limit = 3;
        List<UpcomingBookingVO> mockResponse = List.of(
                UpcomingBookingVO.builder()
                        .id(1L)
                        .specialistName("Schedule Dev Specialist")
                        .serviceName("Counseling")
                        .startTime(LocalDateTime.of(2026, 4, 4, 10, 0, 0))
                        .today(true)
                        .status("CONFIRMED")
                        .build(),
                UpcomingBookingVO.builder()
                        .id(2L)
                        .specialistName("Dr. Adam Smith")
                        .serviceName("Career Planning")
                        .startTime(LocalDateTime.of(2026, 4, 4, 14, 0, 0))
                        .today(true)
                        .status("CONFIRMED")
                        .build(),
                UpcomingBookingVO.builder()
                        .id(3L)
                        .specialistName("Schedule Dev Specialist")
                        .serviceName("Counseling")
                        .startTime(LocalDateTime.of(2026, 4, 5, 9, 0, 0))
                        .today(false)
                        .status("CONFIRMED")
                        .build()
        );
        when(bookingMapper.selectUpcomingBookings(eq(customerId), anyString(), any(LocalDateTime.class), eq(limit)))
                .thenReturn(mockResponse);

        // Act
        List<UpcomingBookingVO> result = bookingService.getUpcomingBookingsByCustomer(customerId, limit);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Schedule Dev Specialist", result.get(0).getSpecialistName());
        assertEquals("Counseling", result.get(0).getServiceName());
        assertEquals(LocalDateTime.of(2026, 4, 4, 10, 0, 0), result.get(0).getStartTime());
        assertTrue(result.get(0).getToday());
        assertEquals("CONFIRMED", result.get(0).getStatus());
        assertEquals("Dr. Adam Smith", result.get(1).getSpecialistName());
        assertEquals("Career Planning", result.get(1).getServiceName());
        assertEquals(LocalDateTime.of(2026, 4, 4, 14, 0, 0), result.get(1).getStartTime());
        assertTrue(result.get(1).getToday());
        assertEquals("CONFIRMED", result.get(1).getStatus());
        assertEquals("Schedule Dev Specialist", result.get(2).getSpecialistName());
        assertEquals("Counseling", result.get(2).getServiceName());
        assertEquals(LocalDateTime.of(2026, 4, 5, 9, 0, 0), result.get(2).getStartTime());
        assertFalse(result.get(2).getToday());
        assertEquals("CONFIRMED", result.get(2).getStatus());
    }


    @Test
    public void createBooking_Success() {
        Long customerId = 1L;
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic(" Career Planning ");
        createDTO.setCustomerNotes("Need help with internship planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("88.50"));

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);
        when(specialistQueryService.getSpecialistDetail(1L)).thenReturn(specialist);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingMapper.insert(bookingCaptor.capture())).thenAnswer(invocation -> {
            Booking booking = bookingCaptor.getValue();
            booking.setId(99L);
            return 1;
        });

        BookingCreateVO result = bookingService.createBooking(customerId, createDTO);

        assertNotNull(result);
        assertEquals(99L, result.getBookingId());
        assertEquals(BookingStatusEnum.PENDING.name(), result.getStatus());

        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(customerId, savedBooking.getCustomerId());
        assertEquals(1L, savedBooking.getSpecialistId());
        assertEquals(11L, savedBooking.getSlotId());
        assertEquals("Career Planning", savedBooking.getTopic());
        assertEquals(new BigDecimal("88.50"), savedBooking.getPrice());

        assertEquals(TimeSlotStatusEnum.BOOKED.name(), slot.getStatus());
        verify(timeSlotMapper).update(any(TimeSlot.class), any());
    }

    @Test
    public void createBooking_TimeSlotNotFound() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        when(timeSlotMapper.selectById(11L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
        assertEquals("Time slot not found", exception.getMessage());
    }

    @Test
    public void createBooking_SlotBelongsToAnotherSpecialist() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(2L);

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Slot does not belong to the selected specialist", exception.getMessage());
    }

    @Test
    public void createBooking_TimeSlotAlreadyBooked() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Time slot already booked", exception.getMessage());
    }

    @Test
    public void createBooking_TopicNotAllowed() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Stress Management");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Topic is not available for this specialist", exception.getMessage());
    }

    @Test
    public void createBooking_ConcurrentUpdateFails() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(BigDecimal.TEN);

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);
        when(specialistQueryService.getSpecialistDetail(1L)).thenReturn(specialist);
        when(bookingMapper.insert(any(Booking.class))).thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Time slot already booked", exception.getMessage());
    }

    @Test
    void testGetBookingList_Upcoming() {
        BookingPageQueryDTO dto = new BookingPageQueryDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setTab("UPCOMING");
        dto.setStatus(null);

        List<BookingItemVO> mockList = List.of(new BookingItemVO(), new BookingItemVO());
        when(bookingMapper.selectBookingList(eq(1L), eq("UPCOMING"), eq(null), any(LocalDateTime.class), eq(0L), eq(10)))
                .thenReturn(mockList);
        when(bookingMapper.selectBookingListCount(eq(1L), eq("UPCOMING"), eq(null), any(LocalDateTime.class)))
                .thenReturn(2L);

        PageResult<BookingItemVO> result = bookingService.getBookingList(1L, dto);

        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getList().size());
        verify(bookingMapper, times(1)).selectBookingList(eq(1L), eq("UPCOMING"), eq(null), any(LocalDateTime.class), eq(0L), eq(10));
        verify(bookingMapper, times(1)).selectBookingListCount(eq(1L), eq("UPCOMING"), eq(null), any(LocalDateTime.class));
    }

    @Test
    void testGetBookingList_History_Empty() {
        BookingPageQueryDTO dto = new BookingPageQueryDTO();
        dto.setPageNo(2);
        dto.setPageSize(5);
        dto.setTab("HISTORY");
        dto.setStatus(null);

        when(bookingMapper.selectBookingList(eq(1L), eq("HISTORY"), eq(null), any(LocalDateTime.class), eq(5L), eq(5)))
                .thenReturn(List.of());
        when(bookingMapper.selectBookingListCount(eq(1L), eq("HISTORY"), eq(null), any(LocalDateTime.class)))
                .thenReturn(0L);

        PageResult<BookingItemVO> result = bookingService.getBookingList(1L, dto);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getList().isEmpty());
        verify(bookingMapper, times(1)).selectBookingList(eq(1L), eq("HISTORY"), eq(null), any(LocalDateTime.class), eq(5L), eq(5));
        verify(bookingMapper, times(1)).selectBookingListCount(eq(1L), eq("HISTORY"), eq(null), any(LocalDateTime.class));
    }
}
