package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingTopicMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.DashboardQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.UsageSummaryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardHabitRawVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardStatisticsVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.CustomerBookingChangePolicyService;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UsageSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    private BookingTopicMapper bookingTopicMapper;

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @Mock
    private SpecialistQueryService specialistQueryService;

    @Mock
    private CustomerBookingChangePolicyService customerBookingChangePolicyService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    public void getUpcomingBookingsByCustomer_Success() {
        // Arrange
        Long customerId = 1L;
        int limit = 3;
        LocalDate today = LocalDate.now();
        LocalDateTime firstBookingTime = today.atTime(10, 0);
        LocalDateTime secondBookingTime = today.atTime(14, 0);
        LocalDateTime thirdBookingTime = today.plusDays(1).atTime(10, 0);
        List<UpcomingBookingVO> mockResponse = List.of(
                UpcomingBookingVO.builder()
                        .id(1L)
                        .specialistName("Schedule Dev Specialist")
                        .serviceName("Counseling")
                        .startTime(firstBookingTime)
                        .today(null)
                        .status("CONFIRMED")
                        .build(),
                UpcomingBookingVO.builder()
                        .id(2L)
                        .specialistName("Dr. Adam Smith")
                        .serviceName("Career Planning")
                        .startTime(secondBookingTime)
                        .today(null)
                        .status("CONFIRMED")
                        .build(),
                UpcomingBookingVO.builder()
                        .id(3L)
                        .specialistName("Schedule Dev Specialist")
                        .serviceName("Counseling")
                        .startTime(thirdBookingTime)
                        .today(null)
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
        assertEquals(firstBookingTime, result.get(0).getStartTime());
        assertTrue(result.get(0).getToday());
        assertEquals("CONFIRMED", result.get(0).getStatus());
        assertEquals("Dr. Adam Smith", result.get(1).getSpecialistName());
        assertEquals("Career Planning", result.get(1).getServiceName());
        assertEquals(secondBookingTime, result.get(1).getStartTime());
        assertTrue(result.get(1).getToday());
        assertEquals("CONFIRMED", result.get(1).getStatus());
        assertEquals("Schedule Dev Specialist", result.get(2).getSpecialistName());
        assertEquals("Counseling", result.get(2).getServiceName());
        assertEquals(thirdBookingTime, result.get(2).getStartTime());
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
        when(bookingTopicMapper.countActiveTopicByName("Career Planning")).thenReturn(1L);
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
        // explicitly mark this topic as not allowed
        when(bookingTopicMapper.countActiveTopicByName("Stress Management")).thenReturn(0L);

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
        when(bookingTopicMapper.countActiveTopicByName("Career Planning")).thenReturn(1L);
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

    @Test
    void testGetBookingList_WithStatusFilter() {
        BookingPageQueryDTO dto = new BookingPageQueryDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setTab("HISTORY");
        dto.setStatus("COMPLETED");

        List<BookingItemVO> mockList = List.of(new BookingItemVO());
        when(bookingMapper.selectBookingList(eq(1L), eq("HISTORY"), eq("COMPLETED"), any(LocalDateTime.class), eq(0L), eq(10)))
                .thenReturn(mockList);
        when(bookingMapper.selectBookingListCount(eq(1L), eq("HISTORY"), eq("COMPLETED"), any(LocalDateTime.class)))
                .thenReturn(1L);

        PageResult<BookingItemVO> result = bookingService.getBookingList(1L, dto);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        verify(bookingMapper, times(1)).selectBookingList(eq(1L), eq("HISTORY"), eq("COMPLETED"), any(LocalDateTime.class), eq(0L), eq(10));
        verify(bookingMapper, times(1)).selectBookingListCount(eq(1L), eq("HISTORY"), eq("COMPLETED"), any(LocalDateTime.class));
    }

    @Test
    void testGetBookingList_AllBookings() {
        BookingPageQueryDTO dto = new BookingPageQueryDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setTab(null);
        dto.setStatus(null);

        List<BookingItemVO> mockList = List.of(new BookingItemVO(), new BookingItemVO());
        when(bookingMapper.selectBookingList(eq(1L), eq(null), eq(null), any(LocalDateTime.class), eq(0L), eq(10)))
                .thenReturn(mockList);
        when(bookingMapper.selectBookingListCount(eq(1L), eq(null), eq(null), any(LocalDateTime.class)))
                .thenReturn(2L);

        PageResult<BookingItemVO> result = bookingService.getBookingList(1L, dto);

        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getList().size());
        verify(bookingMapper, times(1)).selectBookingList(eq(1L), eq(null), eq(null), any(LocalDateTime.class), eq(0L), eq(10));
        verify(bookingMapper, times(1)).selectBookingListCount(eq(1L), eq(null), eq(null), any(LocalDateTime.class));
    }

    @Test
    void testGetBookingList_UpcomingWithStatus() {
        BookingPageQueryDTO dto = new BookingPageQueryDTO();
        dto.setPageNo(1);
        dto.setPageSize(5);
        dto.setTab("UPCOMING");
        dto.setStatus("PENDING");

        List<BookingItemVO> mockList = List.of(new BookingItemVO());
        when(bookingMapper.selectBookingList(eq(1L), eq("UPCOMING"), eq("PENDING"), any(LocalDateTime.class), eq(0L), eq(5)))
                .thenReturn(mockList);
        when(bookingMapper.selectBookingListCount(eq(1L), eq("UPCOMING"), eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(1L);

        PageResult<BookingItemVO> result = bookingService.getBookingList(1L, dto);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        verify(bookingMapper, times(1)).selectBookingList(eq(1L), eq("UPCOMING"), eq("PENDING"), any(LocalDateTime.class), eq(0L), eq(5));
        verify(bookingMapper, times(1)).selectBookingListCount(eq(1L), eq("UPCOMING"), eq("PENDING"), any(LocalDateTime.class));
    }


    @Test
    void testGetBookingDetail_Success() {
        Long bookingId = 100L;

        BookingDetailVO vo = new BookingDetailVO();
        vo.setBookingId(bookingId);
        vo.setStatus("CONFIRMED");
        vo.setSpecialistId(10L);
        vo.setSpecialistName("Dr. Test");
        vo.setSpecialistAvatar("http://example.com/a.png");
        vo.setSlotDate("2026-04-15");
        vo.setStartTime("10:00");
        vo.setEndTime("11:00");
        vo.setPrice(new BigDecimal("100.00"));
        vo.setTopic("Topic");
        vo.setCustomerNotes("Notes");

        when(bookingMapper.selectBookingDetailById(eq(bookingId))).thenReturn(Optional.of(vo));

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        when(bookingMapper.selectById(eq(bookingId))).thenReturn(booking);

        BookingDetailVO result = bookingService.getBookingDetailById(bookingId, 1L);

        assertNotNull(result);
        assertEquals(bookingId, result.getBookingId());
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("Dr. Test", result.getSpecialistName());
        assertEquals(new BigDecimal("100.00"), result.getPrice());
    }

    @Test
    void testGetBookingDetail_NotFound() {
        Long bookingId = 101L;
        when(bookingMapper.selectBookingDetailById(eq(bookingId))).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.getBookingDetailById(bookingId, 1L));
        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testGetBookingDetail_Forbidden() {
        Long bookingId = 102L;

        BookingDetailVO vo = new BookingDetailVO();
        vo.setBookingId(bookingId);
        when(bookingMapper.selectBookingDetailById(eq(bookingId))).thenReturn(Optional.of(vo));

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(999L); // belongs to another user
        when(bookingMapper.selectById(eq(bookingId))).thenReturn(booking);

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.getBookingDetailById(bookingId, 1L));
        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test

    void customerCancellationQuote_DelegatesToPolicy() {
        Long bookingId = 200L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSlotId(55L);
        booking.setStatus("CONFIRMED");
        booking.setPrice(new BigDecimal("120.00"));

        TimeSlot slot = new TimeSlot();
        slot.setId(55L);
        slot.setSlotDate(LocalDate.of(2026, 5, 1));
        slot.setStartTime(LocalTime.of(14, 0));

        BookingCancelQuoteVO quoted = BookingCancelQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .bookingStartAt(LocalDateTime.of(2026, 5, 1, 14, 0))
                .orderAmount(new BigDecimal("120.00"))
                .refundAmount(new BigDecimal("120.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(55L)).thenReturn(slot);
        when(customerBookingChangePolicyService.customerCancellationQuote(
                eq("CONFIRMED"),
                eq(LocalDateTime.of(2026, 5, 1, 14, 0)),
                any(LocalDateTime.class),
                eq(new BigDecimal("120.00"))))
                .thenReturn(quoted);

        BookingCancelQuoteVO result = bookingService.customerCancellationQuote(bookingId, 1L);
        assertTrue(result.isAllowed());
        assertEquals("FULL_REFUND", result.getPolicyType());
    }

    @Test
    void customerCancellationQuote_BookingNotFound() {
        when(bookingMapper.selectById(201L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerCancellationQuote(201L, 1L));
        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void customerCancellationQuote_Forbidden() {
        Booking booking = new Booking();
        booking.setId(202L);
        booking.setCustomerId(99L);
        when(bookingMapper.selectById(202L)).thenReturn(booking);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerCancellationQuote(202L, 1L));
        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void customerRescheduleQuote_DelegatesToPolicy() {
        Long bookingId = 400L;
        Long newSlotId = 77L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSpecialistId(5L);
        booking.setSlotId(55L);
        booking.setStatus("CONFIRMED");
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot currentSlot = new TimeSlot();
        currentSlot.setId(55L);
        currentSlot.setSpecialistId(5L);
        currentSlot.setSlotDate(LocalDate.of(2026, 5, 1));
        currentSlot.setStartTime(LocalTime.of(14, 0));

        TimeSlot newSlot = new TimeSlot();
        newSlot.setId(77L);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));

        BookingRescheduleQuoteVO quoted = BookingRescheduleQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .bookingStartAt(LocalDateTime.of(2026, 5, 1, 14, 0))
                .originalPrice(new BigDecimal("100.00"))
                .newPrice(new BigDecimal("120.00"))
                .priceDifference(new BigDecimal("20.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .refundAmount(new BigDecimal("0.00"))
                .payableAmount(new BigDecimal("20.00"))
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(55L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(77L)).thenReturn(newSlot);
        when(specialistQueryService.getSpecialistDetail(5L)).thenReturn(specialist);
        when(customerBookingChangePolicyService.customerRescheduleQuote(
                eq("CONFIRMED"),
                eq(LocalDateTime.of(2026, 5, 1, 14, 0)),
                any(LocalDateTime.class),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("120.00"))))
                .thenReturn(quoted);

        BookingRescheduleQuoteVO result = bookingService.customerRescheduleQuote(bookingId, newSlotId, 1L);
        assertTrue(result.isAllowed());
        assertEquals(new BigDecimal("20.00"), result.getPayableAmount());
    }

    @Test
    void customerRescheduleConfirm_Success() {
        Long bookingId = 401L;
        Long newSlotId = 78L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSpecialistId(5L);
        booking.setSlotId(55L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot currentSlot = new TimeSlot();
        currentSlot.setId(55L);
        currentSlot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        currentSlot.setSlotDate(LocalDate.of(2026, 5, 1));
        currentSlot.setStartTime(LocalTime.of(14, 0));

        TimeSlot newSlot = new TimeSlot();
        newSlot.setId(newSlotId);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));

        BookingRescheduleQuoteVO quoted = BookingRescheduleQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .message("More than 24 hours to start, no reschedule penalty")
                .priceDifference(new BigDecimal("20.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .refundAmount(new BigDecimal("0.00"))
                .payableAmount(new BigDecimal("20.00"))
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(55L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(newSlotId)).thenReturn(newSlot);
        when(specialistQueryService.getSpecialistDetail(5L)).thenReturn(specialist);
        when(customerBookingChangePolicyService.customerRescheduleQuote(
                eq(BookingStatusEnum.CONFIRMED.name()),
                eq(LocalDateTime.of(2026, 5, 1, 14, 0)),
                any(LocalDateTime.class),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("120.00"))))
                .thenReturn(quoted);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);

        BookingRescheduleConfirmVO result = bookingService.customerRescheduleConfirm(bookingId, newSlotId, 1L);

        assertNotNull(result);
        assertEquals(bookingId, result.getBookingId());
        assertEquals(BookingStatusEnum.PENDING.name(), result.getBookingStatus());
        assertEquals("FULL_REFUND", result.getPolicyType());
        assertEquals(new BigDecimal("20.00"), result.getPayableAmount());
        verify(bookingMapper).updateById(argThat(updated ->
                newSlotId.equals(updated.getSlotId())
                        && "RESCHEDULE".equals(updated.getChangeType())
                        && BookingStatusEnum.PENDING.name().equals(updated.getStatus())
        ));
    }

    @Test
    void customerRescheduleConfirm_NewSlotUpdateFails() {
        Long bookingId = 402L;
        Long newSlotId = 79L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSpecialistId(5L);
        booking.setSlotId(56L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot currentSlot = new TimeSlot();
        currentSlot.setId(56L);
        currentSlot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        currentSlot.setSlotDate(LocalDate.of(2026, 5, 2));
        currentSlot.setStartTime(LocalTime.of(10, 0));

        TimeSlot newSlot = new TimeSlot();
        newSlot.setId(newSlotId);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));

        BookingRescheduleQuoteVO quoted = BookingRescheduleQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(56L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(newSlotId)).thenReturn(newSlot);
        when(specialistQueryService.getSpecialistDetail(5L)).thenReturn(specialist);
        when(customerBookingChangePolicyService.customerRescheduleQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(quoted);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1, 0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerRescheduleConfirm(bookingId, newSlotId, 1L));
        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), ex.getCode());
        assertEquals("Time slot is not available", ex.getMessage());
    }

    @Test
    void customerCancellationConfirm_Success() {
        Long bookingId = 300L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSlotId(88L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("120.00"));

        TimeSlot slot = new TimeSlot();
        slot.setId(88L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        slot.setSlotDate(LocalDate.of(2026, 5, 1));
        slot.setStartTime(LocalTime.of(14, 0));

        BookingCancelQuoteVO quoted = BookingCancelQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .message("More than 24 hours to start, full refund")
                .refundAmount(new BigDecimal("120.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(88L)).thenReturn(slot);
        when(customerBookingChangePolicyService.customerCancellationQuote(
                eq(BookingStatusEnum.CONFIRMED.name()),
                eq(LocalDateTime.of(2026, 5, 1, 14, 0)),
                any(LocalDateTime.class),
                eq(new BigDecimal("120.00")))).thenReturn(quoted);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);

        BookingCancelConfirmVO result = bookingService.customerCancellationConfirm(bookingId, 1L);

        assertEquals(bookingId, result.getBookingId());
        assertEquals(BookingStatusEnum.CANCELLED.name(), result.getBookingStatus());
        assertEquals(new BigDecimal("120.00"), result.getRefundAmount());
        assertEquals(new BigDecimal("0.00"), result.getPenaltyAmount());
        verify(bookingMapper).updateById(argThat(updated ->
                BookingStatusEnum.CANCELLED.name().equals(updated.getStatus())
                        && "CUSTOMER".equals(updated.getCancelledBy())
                        && "CANCEL".equals(updated.getChangeType())
        ));
    }

    @Test
    void customerCancellationConfirm_NotAllowedByPolicy() {
        Long bookingId = 301L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSlotId(89L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot slot = new TimeSlot();
        slot.setId(89L);
        slot.setSlotDate(LocalDate.of(2026, 5, 1));
        slot.setStartTime(LocalTime.of(9, 0));

        BookingCancelQuoteVO quoted = BookingCancelQuoteVO.builder()
                .allowed(false)
                .message("Less than 2 hours to start, cannot cancel or reschedule")
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(89L)).thenReturn(slot);
        when(customerBookingChangePolicyService.customerCancellationQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class)))
                .thenReturn(quoted);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerCancellationConfirm(bookingId, 1L));
        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        verify(bookingMapper, times(0)).updateById(any(Booking.class));

    }

    @Test
    void testGetUsageSummary_AllTime_DefaultsWhenMapperReturnsNull() {
        Long customerId = 1L;
        UserContextHolder.setUserId(customerId);

        when(bookingMapper.selectUsageSummary(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull()))
                .thenReturn(null);
        when(bookingMapper.selectConsultedExperts(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull()))
                .thenReturn(null);

        UsageSummaryVO result = bookingService.getUsageSummary(null);

        assertNotNull(result);
        assertEquals(0, result.getTotalCompletedAppointments());
        assertEquals(0, result.getTotalConsultationHours());
        assertNotNull(result.getTotalAmountSpent());
        assertEquals(0, result.getTotalAmountSpent().compareTo(BigDecimal.ZERO));
        assertNotNull(result.getConsultedExperts());
        assertTrue(result.getConsultedExperts().isEmpty());

        verify(bookingMapper, times(1)).selectUsageSummary(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull());
        verify(bookingMapper, times(1)).selectConsultedExperts(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull());
    }

    @Test
    void testGetUsageSummary_WithDateRange_PropagatesDatesAndNormalizesNullTotals() {
        Long customerId = 2L;
        UserContextHolder.setUserId(customerId);

        UsageSummaryQueryDTO queryDTO = new UsageSummaryQueryDTO();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        queryDTO.setStartDate(startDate);
        queryDTO.setEndDate(endDate);

        UsageSummaryVO mapperSummary = new UsageSummaryVO();
        mapperSummary.setTotalCompletedAppointments(null);
        mapperSummary.setTotalAmountSpent(null);
        mapperSummary.setTotalConsultationHours(null);

        UsageSummaryVO.ConsultedExpertVO expert = new UsageSummaryVO.ConsultedExpertVO();
        expert.setSpecialistId(10L);
        expert.setSpecialistName("specialist1");
        List<UsageSummaryVO.ConsultedExpertVO> experts = new ArrayList<>();
        experts.add(expert);

        when(bookingMapper.selectUsageSummary(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate)))
                .thenReturn(mapperSummary);
        when(bookingMapper.selectConsultedExperts(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate)))
                .thenReturn(experts);

        UsageSummaryVO result = bookingService.getUsageSummary(queryDTO);

        assertNotNull(result);
        assertEquals(0, result.getTotalCompletedAppointments());
        assertEquals(0, result.getTotalConsultationHours());
        assertNotNull(result.getTotalAmountSpent());
        assertEquals(0, result.getTotalAmountSpent().compareTo(BigDecimal.ZERO));
        assertNotNull(result.getConsultedExperts());
        assertEquals(1, result.getConsultedExperts().size());
        assertEquals(10L, result.getConsultedExperts().get(0).getSpecialistId());
        assertEquals("specialist1", result.getConsultedExperts().get(0).getSpecialistName());

        verify(bookingMapper, times(1)).selectUsageSummary(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate));
        verify(bookingMapper, times(1)).selectConsultedExperts(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate));
    }

    @Test
    void testGetUsageSummary_InvalidDateRange_ThrowsParamError() {
        UserContextHolder.setUserId(3L);

        UsageSummaryQueryDTO queryDTO = new UsageSummaryQueryDTO();
        queryDTO.setStartDate(LocalDate.of(2026, 2, 1));
        queryDTO.setEndDate(LocalDate.of(2026, 1, 1));

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.getUsageSummary(queryDTO));
        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void testGetDashboardStatistics_AllTime_UsesMonthlyTrendAndPadsHabitData() {
        Long customerId = 11L;
        UserContextHolder.setUserId(customerId);

        DashboardStatisticsVO summary = new DashboardStatisticsVO();
        summary.setTotalCompletedAppointments(null);
        summary.setTotalAmountSpent(null);
        summary.setTotalConsultationHours(null);

        DashboardStatisticsVO.ConsultedExpertVO expert = new DashboardStatisticsVO.ConsultedExpertVO();
        expert.setSpecialistId(20L);
        expert.setSpecialistName("Dr. Alpha");

        DashboardStatisticsVO.TrendChartVO monthTrend = new DashboardStatisticsVO.TrendChartVO();
        monthTrend.setDateLabel("2026-04");
        monthTrend.setCount(2);
        monthTrend.setHours(1.5D);

        DashboardStatisticsVO.CategoryChartVO category = new DashboardStatisticsVO.CategoryChartVO();
        category.setCategoryName("Psychology");
        category.setAmount(new BigDecimal("120.00"));
        category.setCount(2);

        DashboardHabitRawVO mondayRaw = new DashboardHabitRawVO();
        mondayRaw.setDayOfWeek(2);
        mondayRaw.setCount(3);
        DashboardHabitRawVO sundayRaw = new DashboardHabitRawVO();
        sundayRaw.setDayOfWeek(1);
        sundayRaw.setCount(1);

        when(bookingMapper.selectDashboardSummary(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull()))
                .thenReturn(summary);
        when(bookingMapper.selectDashboardConsultedExperts(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull()))
                .thenReturn(List.of(expert));
        when(bookingMapper.selectDashboardTrendByMonth(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull()))
                .thenReturn(List.of(monthTrend));
        when(bookingMapper.selectDashboardCategoryData(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull()))
                .thenReturn(List.of(category));
        when(bookingMapper.selectDashboardHabitData(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull()))
                .thenReturn(List.of(mondayRaw, sundayRaw));

        DashboardStatisticsVO result = bookingService.getDashboardStatistics(null);

        assertNotNull(result);
        assertEquals(0, result.getTotalCompletedAppointments());
        assertEquals(0, result.getTotalAmountSpent().compareTo(BigDecimal.ZERO));
        assertEquals(0.0D, result.getTotalConsultationHours());
        assertEquals(1, result.getConsultedExperts().size());
        assertEquals(1, result.getTrendData().size());
        assertEquals(1, result.getCategoryData().size());
        assertEquals(7, result.getHabitData().size());
        assertEquals("Mon", result.getHabitData().get(0).getDayOfWeek());
        assertEquals(3, result.getHabitData().get(0).getCount());
        assertEquals("Sun", result.getHabitData().get(6).getDayOfWeek());
        assertEquals(1, result.getHabitData().get(6).getCount());
        assertEquals(0, result.getHabitData().get(1).getCount());

        verify(bookingMapper, times(1)).selectDashboardTrendByMonth(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull());
        verify(bookingMapper, never()).selectDashboardTrendByDay(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), isNull(), isNull());
    }

    @Test
    void testGetDashboardStatistics_WithShortDateRange_UsesDailyTrend() {
        Long customerId = 12L;
        UserContextHolder.setUserId(customerId);

        DashboardQueryDTO queryDTO = new DashboardQueryDTO();
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 14);
        queryDTO.setStartDate(startDate);
        queryDTO.setEndDate(endDate);

        DashboardStatisticsVO.TrendChartVO dayTrend = new DashboardStatisticsVO.TrendChartVO();
        dayTrend.setDateLabel("04-14");
        dayTrend.setCount(1);
        dayTrend.setHours(0.5D);

        when(bookingMapper.selectDashboardSummary(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate)))
                .thenReturn(new DashboardStatisticsVO());
        when(bookingMapper.selectDashboardConsultedExperts(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate)))
                .thenReturn(List.of());
        when(bookingMapper.selectDashboardTrendByDay(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate)))
                .thenReturn(List.of(dayTrend));
        when(bookingMapper.selectDashboardCategoryData(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate)))
                .thenReturn(List.of());
        when(bookingMapper.selectDashboardHabitData(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate)))
                .thenReturn(List.of());

        DashboardStatisticsVO result = bookingService.getDashboardStatistics(queryDTO);

        assertNotNull(result);
        assertEquals(1, result.getTrendData().size());
        assertEquals("04-14", result.getTrendData().get(0).getDateLabel());

        verify(bookingMapper, times(1)).selectDashboardTrendByDay(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate));
        verify(bookingMapper, never()).selectDashboardTrendByMonth(eq(customerId), eq(BookingStatusEnum.COMPLETED.name()), eq(startDate), eq(endDate));
    }

    @Test
    void testGetDashboardStatistics_InvalidDateRange_ThrowsParamError() {
        UserContextHolder.setUserId(13L);

        DashboardQueryDTO queryDTO = new DashboardQueryDTO();
        queryDTO.setStartDate(LocalDate.of(2026, 4, 20));
        queryDTO.setEndDate(LocalDate.of(2026, 4, 10));

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.getDashboardStatistics(queryDTO));
        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

}
