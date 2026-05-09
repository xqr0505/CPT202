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
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistForceCancelBookingRequestDTO;
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
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * @author QiranXiao
 * @since 2026/4/1
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

    @Mock
    private RedisTemplate<String, Object> jsonRedisTemplate;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    private static LocalDate futureDate(int days) {
        return LocalDate.now().plusDays(days);
    }

    private static void populateFutureSlot(TimeSlot slot, int days) {
        slot.setSlotDate(futureDate(days));
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
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
    void autoCompleteExpiredConfirmedBookings_CompletesEndedConfirmedBookings() {
        Booking first = new Booking();
        first.setId(101L);
        first.setCustomerId(11L);
        first.setStatus(BookingStatusEnum.CONFIRMED.name());

        Booking second = new Booking();
        second.setId(102L);
        second.setCustomerId(12L);
        second.setStatus(BookingStatusEnum.CONFIRMED.name());

        when(bookingMapper.selectAutoCompletableConfirmedBookings(
                eq(BookingStatusEnum.CONFIRMED.name()),
                eq(TimeSlotStatusEnum.BOOKED.name()),
                any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));
        when(bookingMapper.updateStatusIfCurrent(101L, BookingStatusEnum.CONFIRMED.name(), BookingStatusEnum.COMPLETED.name()))
                .thenReturn(1);
        when(bookingMapper.updateStatusIfCurrent(102L, BookingStatusEnum.CONFIRMED.name(), BookingStatusEnum.COMPLETED.name()))
                .thenReturn(1);
        when(jsonRedisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        int result = bookingService.autoCompleteExpiredConfirmedBookings();

        assertEquals(2, result);
        verify(bookingMapper).updateStatusIfCurrent(101L, BookingStatusEnum.CONFIRMED.name(), BookingStatusEnum.COMPLETED.name());
        verify(bookingMapper).updateStatusIfCurrent(102L, BookingStatusEnum.CONFIRMED.name(), BookingStatusEnum.COMPLETED.name());
        verify(jsonRedisTemplate, times(2)).keys(anyString());
    }

    @Test
    void autoCompleteExpiredConfirmedBookings_DoesNotInvalidateCacheWhenNothingChanges() {
        Booking booking = new Booking();
        booking.setId(103L);
        booking.setCustomerId(13L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());

        when(bookingMapper.selectAutoCompletableConfirmedBookings(
                eq(BookingStatusEnum.CONFIRMED.name()),
                eq(TimeSlotStatusEnum.BOOKED.name()),
                any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingMapper.updateStatusIfCurrent(103L, BookingStatusEnum.CONFIRMED.name(), BookingStatusEnum.COMPLETED.name()))
                .thenReturn(0);

        int result = bookingService.autoCompleteExpiredConfirmedBookings();

        assertEquals(0, result);
        verify(jsonRedisTemplate, never()).keys(anyString());
        verify(jsonRedisTemplate, never()).delete(anySet());
    }

    @Test
    void autoCancelExpiredPendingBookings_CancelsTimedOutRequests() {
        ReflectionTestUtils.setField(bookingService, "specialistApprovalTimeoutMinutes", 1440L);

        SpecialistPendingBookingVO timedOut = new SpecialistPendingBookingVO();
        timedOut.setId(201L);
        timedOut.setSubmissionTime(LocalDateTime.now().minusDays(2));
        timedOut.setRequestedStartTime(LocalDateTime.now().plusDays(1));

        Booking booking = new Booking();
        booking.setId(201L);
        booking.setStatus(BookingStatusEnum.PENDING.name());
        booking.setSlotId(301L);
        booking.setCustomerId(21L);
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot slot = new TimeSlot();
        slot.setId(301L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());

        when(bookingMapper.selectExpiredPendingRequests(eq(BookingStatusEnum.PENDING.name()), eq(1440L)))
                .thenReturn(List.of(timedOut));
        when(bookingMapper.selectById(201L)).thenReturn(booking);
        when(timeSlotMapper.selectById(301L)).thenReturn(slot);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);
        when(jsonRedisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        int result = bookingService.autoCancelExpiredPendingBookings();

        assertEquals(1, result);
        verify(bookingMapper).updateById(any(Booking.class));
        verify(bookingMapper).insertRefundPenaltyRecord(eq(201L), eq(new BigDecimal("100.00")), eq(BigDecimal.ZERO), eq("SYSTEM_TIMEOUT_FULL_REFUND"), eq("PENDING"));
    }

    @Test
    void autoCancelExpiredPendingBookings_DoesNotFailWhenSlotAlreadyAvailable() {
        ReflectionTestUtils.setField(bookingService, "specialistApprovalTimeoutMinutes", 1440L);

        SpecialistPendingBookingVO timedOut = new SpecialistPendingBookingVO();
        timedOut.setId(202L);
        timedOut.setSubmissionTime(LocalDateTime.now().minusDays(2));
        timedOut.setRequestedStartTime(LocalDateTime.now().plusDays(1));

        Booking booking = new Booking();
        booking.setId(202L);
        booking.setStatus(BookingStatusEnum.PENDING.name());
        booking.setSlotId(302L);
        booking.setCustomerId(22L);
        booking.setPrice(new BigDecimal("165.00"));

        TimeSlot slot = new TimeSlot();
        slot.setId(302L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        when(bookingMapper.selectExpiredPendingRequests(eq(BookingStatusEnum.PENDING.name()), eq(1440L)))
                .thenReturn(List.of(timedOut));
        when(bookingMapper.selectById(202L)).thenReturn(booking);
        when(timeSlotMapper.selectById(302L)).thenReturn(slot);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(0);
        when(jsonRedisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        int result = bookingService.autoCancelExpiredPendingBookings();

        assertEquals(1, result);
        verify(bookingMapper).updateById(any(Booking.class));
        verify(bookingMapper).insertRefundPenaltyRecord(eq(202L), eq(new BigDecimal("165.00")), eq(BigDecimal.ZERO), eq("SYSTEM_TIMEOUT_FULL_REFUND"), eq("PENDING"));
    }

    @Test
    void autoCancelExpiredPendingBookings_ReturnsZeroWhenNoCandidates() {
        ReflectionTestUtils.setField(bookingService, "specialistApprovalTimeoutMinutes", 1440L);

        when(bookingMapper.selectExpiredPendingRequests(eq(BookingStatusEnum.PENDING.name()), eq(1440L)))
                .thenReturn(Collections.emptyList());

        int result = bookingService.autoCancelExpiredPendingBookings();

        assertEquals(0, result);
        verify(bookingMapper, never()).updateById(any(Booking.class));
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
        populateFutureSlot(slot, 2);

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("88.50"));
        specialist.setStatus("ACTIVE");

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);
        when(specialistQueryService.getSpecialistDetail(1L)).thenReturn(specialist);
        when(bookingTopicMapper.countActiveTopicByName("Career Planning")).thenReturn(1L);
        when(bookingMapper.selectOne(any())).thenReturn(null);
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
    public void createBooking_ReusesCancelledBookingOnSameSlot() {
        Long customerId = 66L;
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(2L);
        createDTO.setSlotId(21L);
        createDTO.setTopic("Initial Consultation");
        createDTO.setCustomerNotes("Need follow-up plan");

        TimeSlot slot = new TimeSlot();
        slot.setId(21L);
        slot.setSpecialistId(2L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        populateFutureSlot(slot, 2);

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("170.00"));
        specialist.setStatus("ACTIVE");

        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(501L);
        cancelledBooking.setSlotId(21L);
        cancelledBooking.setStatus(BookingStatusEnum.CANCELLED.name());
        cancelledBooking.setCustomerId(9L);
        cancelledBooking.setSpecialistId(2L);

        when(timeSlotMapper.selectById(21L)).thenReturn(slot);
        when(specialistQueryService.getSpecialistDetail(2L)).thenReturn(specialist);
        when(bookingTopicMapper.countActiveTopicByName("Initial Consultation")).thenReturn(1L);
        when(bookingMapper.selectOne(any())).thenReturn(cancelledBooking);
        when(bookingMapper.updateById(cancelledBooking)).thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);

        BookingCreateVO result = bookingService.createBooking(customerId, createDTO);

        assertNotNull(result);
        assertEquals(501L, result.getBookingId());
        assertEquals(BookingStatusEnum.PENDING.name(), result.getStatus());
        assertEquals(customerId, cancelledBooking.getCustomerId());
        assertEquals("Initial Consultation", cancelledBooking.getTopic());
        assertEquals("Need follow-up plan", cancelledBooking.getCustomerNotes());
        assertEquals(new BigDecimal("170.00"), cancelledBooking.getPrice());
        assertEquals(TimeSlotStatusEnum.BOOKED.name(), slot.getStatus());
        verify(bookingMapper, never()).insert(any(Booking.class));
        verify(bookingMapper).updateById(cancelledBooking);
    }

    @Test
    public void createBooking_ExistingNonCancelledBookingOnSameSlot() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        populateFutureSlot(slot, 2);

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(BigDecimal.TEN);
        specialist.setStatus("ACTIVE");

        Booking existing = new Booking();
        existing.setId(700L);
        existing.setSlotId(11L);
        existing.setStatus(BookingStatusEnum.PENDING.name());

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);
        when(specialistQueryService.getSpecialistDetail(1L)).thenReturn(specialist);
        when(bookingTopicMapper.countActiveTopicByName("Career Planning")).thenReturn(1L);
        when(bookingMapper.selectOne(any())).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Time slot already booked", exception.getMessage());
        verify(bookingMapper, never()).insert(any(Booking.class));
        verify(bookingMapper, never()).updateById(any(Booking.class));
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
        populateFutureSlot(slot, 2);

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
        populateFutureSlot(slot, 2);

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
        populateFutureSlot(slot, 2);

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
        populateFutureSlot(slot, 2);

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(BigDecimal.TEN);
        specialist.setStatus("ACTIVE");

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
    public void createBooking_PastTimeSlotRejected() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        slot.setSlotDate(LocalDate.now().minusDays(1));
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Past time slots cannot be booked", exception.getMessage());
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
    void customerCancellationQuote_BlockedByPolicy_ThrowsParamError() {
        Long bookingId = 203L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSlotId(88L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("120.00"));

        TimeSlot slot = new TimeSlot();
        slot.setId(88L);
        slot.setSlotDate(LocalDate.of(2026, 5, 1));
        slot.setStartTime(LocalTime.of(14, 0));

        BookingCancelQuoteVO quoted = BookingCancelQuoteVO.builder()
                .allowed(false)
                .message("Less than 2 hours to start, cannot cancel or reschedule")
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(88L)).thenReturn(slot);
        when(customerBookingChangePolicyService.customerCancellationQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class)))
                .thenReturn(quoted);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerCancellationQuote(bookingId, 1L));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("Less than 2 hours to start, cannot cancel or reschedule", ex.getMessage());
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
        newSlot.setSlotDate(LocalDate.now().plusDays(3));
        newSlot.setStartTime(LocalTime.of(10, 0));

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));
        specialist.setStatus("ACTIVE");

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
    void customerRescheduleQuote_BlockedByPolicy_ThrowsParamError() {
        Long bookingId = 403L;
        Long newSlotId = 87L;
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
        newSlot.setId(newSlotId);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        newSlot.setSlotDate(LocalDate.now().plusDays(3));
        newSlot.setStartTime(LocalTime.of(10, 0));

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));
        specialist.setStatus("ACTIVE");

        BookingRescheduleQuoteVO quoted = BookingRescheduleQuoteVO.builder()
                .allowed(false)
                .message("Less than 2 hours to start, cannot reschedule")
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(55L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(newSlotId)).thenReturn(newSlot);
        when(specialistQueryService.getSpecialistDetail(5L)).thenReturn(specialist);
        when(customerBookingChangePolicyService.customerRescheduleQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(quoted);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerRescheduleQuote(bookingId, newSlotId, 1L));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("Less than 2 hours to start, cannot reschedule", ex.getMessage());
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
        newSlot.setSlotDate(LocalDate.now().plusDays(3));
        newSlot.setStartTime(LocalTime.of(10, 0));

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));
        specialist.setStatus("ACTIVE");

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
        when(bookingMapper.updateCustomerRescheduleIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(newSlotId),
                eq(BookingStatusEnum.PENDING.name()),
                eq("RESCHEDULE"),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);
        when(jsonRedisTemplate.keys(anyString())).thenReturn(Set.of("booking:customer:1:detail:401"));

        BookingRescheduleConfirmVO result = bookingService.customerRescheduleConfirm(bookingId, newSlotId, 1L);

        assertNotNull(result);
        assertEquals(bookingId, result.getBookingId());
        assertEquals(BookingStatusEnum.PENDING.name(), result.getBookingStatus());
        assertEquals("FULL_REFUND", result.getPolicyType());
        assertEquals(new BigDecimal("20.00"), result.getPayableAmount());
        verify(bookingMapper).updateCustomerRescheduleIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(newSlotId),
                eq(BookingStatusEnum.PENDING.name()),
                eq("RESCHEDULE"),
                any(LocalDateTime.class)
        );
        verify(jsonRedisTemplate).delete(Set.of("booking:customer:1:detail:401"));
    }

    @Test
    void customerRescheduleConfirm_TargetSlotHasCancelledBookingRecord() {
        Long bookingId = 1401L;
        Long newSlotId = 178L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSpecialistId(5L);
        booking.setSlotId(155L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot currentSlot = new TimeSlot();
        currentSlot.setId(155L);
        currentSlot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        currentSlot.setSlotDate(LocalDate.of(2026, 5, 1));
        currentSlot.setStartTime(LocalTime.of(14, 0));

        TimeSlot newSlot = new TimeSlot();
        newSlot.setId(newSlotId);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        newSlot.setSlotDate(LocalDate.now().plusDays(3));
        newSlot.setStartTime(LocalTime.of(10, 0));

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));
        specialist.setStatus("ACTIVE");

        Booking cancelledBookingOnNewSlot = new Booking();
        cancelledBookingOnNewSlot.setId(9001L);
        cancelledBookingOnNewSlot.setSlotId(newSlotId);
        cancelledBookingOnNewSlot.setStatus(BookingStatusEnum.CANCELLED.name());

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
        when(timeSlotMapper.selectById(155L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(newSlotId)).thenReturn(newSlot);
        when(specialistQueryService.getSpecialistDetail(5L)).thenReturn(specialist);
        when(customerBookingChangePolicyService.customerRescheduleQuote(
                eq(BookingStatusEnum.CONFIRMED.name()),
                eq(LocalDateTime.of(2026, 5, 1, 14, 0)),
                any(LocalDateTime.class),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("120.00"))))
                .thenReturn(quoted);
        when(bookingMapper.selectOne(any())).thenReturn(cancelledBookingOnNewSlot);
        when(bookingMapper.deleteById(9001L)).thenReturn(1);
        when(bookingMapper.updateCustomerRescheduleIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(newSlotId),
                eq(BookingStatusEnum.PENDING.name()),
                eq("RESCHEDULE"),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);
        when(jsonRedisTemplate.keys(anyString())).thenReturn(Set.of("booking:customer:1:detail:1401"));

        BookingRescheduleConfirmVO result = bookingService.customerRescheduleConfirm(bookingId, newSlotId, 1L);

        assertNotNull(result);
        assertEquals(bookingId, result.getBookingId());
        assertEquals(BookingStatusEnum.PENDING.name(), result.getBookingStatus());
        verify(bookingMapper).deleteById(9001L);
        verify(bookingMapper).updateCustomerRescheduleIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(newSlotId),
                eq(BookingStatusEnum.PENDING.name()),
                eq("RESCHEDULE"),
                any(LocalDateTime.class)
        );
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
        newSlot.setSlotDate(LocalDate.now().plusDays(3));
        newSlot.setStartTime(LocalTime.of(10, 0));

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));
        specialist.setStatus("ACTIVE");

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
    void customerRescheduleQuote_NewSlotWithinTwoHours_NotAllowed() {
        Long bookingId = 480L;
        Long newSlotId = 180L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSpecialistId(5L);
        booking.setSlotId(58L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot currentSlot = new TimeSlot();
        currentSlot.setId(58L);
        currentSlot.setSpecialistId(5L);
        currentSlot.setSlotDate(LocalDate.now().plusDays(1));
        currentSlot.setStartTime(LocalTime.of(14, 0));

        TimeSlot newSlot = new TimeSlot();
        newSlot.setId(newSlotId);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        LocalDateTime tooSoon = LocalDateTime.now().plusMinutes(90);
        newSlot.setSlotDate(tooSoon.toLocalDate());
        newSlot.setStartTime(tooSoon.toLocalTime());

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(58L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(newSlotId)).thenReturn(newSlot);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerRescheduleQuote(bookingId, newSlotId, 1L));
        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("New slot must be more than 2 hours from now", ex.getMessage());
        verify(customerBookingChangePolicyService, never()).customerRescheduleQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void customerRescheduleConfirm_NewSlotWithinTwoHours_NotAllowed() {
        Long bookingId = 481L;
        Long newSlotId = 181L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSpecialistId(5L);
        booking.setSlotId(59L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot currentSlot = new TimeSlot();
        currentSlot.setId(59L);
        currentSlot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        currentSlot.setSlotDate(LocalDate.now().plusDays(1));
        currentSlot.setStartTime(LocalTime.of(14, 0));

        TimeSlot newSlot = new TimeSlot();
        newSlot.setId(newSlotId);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        LocalDateTime tooSoon = LocalDateTime.now().plusMinutes(90);
        newSlot.setSlotDate(tooSoon.toLocalDate());
        newSlot.setStartTime(tooSoon.toLocalTime());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));
        specialist.setStatus("ACTIVE");

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(59L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(newSlotId)).thenReturn(newSlot);
        when(specialistQueryService.getSpecialistDetail(5L)).thenReturn(specialist);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerRescheduleConfirm(bookingId, newSlotId, 1L));
        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("New slot must be more than 2 hours from now", ex.getMessage());
        verify(customerBookingChangePolicyService, never()).customerRescheduleQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class), any(BigDecimal.class));
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
        when(bookingMapper.updateCustomerCancelIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(BookingStatusEnum.CANCELLED.name()),
                eq("CUSTOMER_MANUAL"),
                eq("CANCEL"),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);
        when(jsonRedisTemplate.keys(anyString())).thenReturn(Set.of("booking:customer:1:list:1"));

        BookingCancelConfirmVO result = bookingService.customerCancellationConfirm(bookingId, 1L);

        assertEquals(bookingId, result.getBookingId());
        assertEquals(BookingStatusEnum.CANCELLED.name(), result.getBookingStatus());
        assertEquals(new BigDecimal("120.00"), result.getRefundAmount());
        assertEquals(new BigDecimal("0.00"), result.getPenaltyAmount());
        verify(bookingMapper).updateCustomerCancelIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(BookingStatusEnum.CANCELLED.name()),
                eq("CUSTOMER_MANUAL"),
                eq("CANCEL"),
                any(LocalDateTime.class)
        );
        verify(jsonRedisTemplate).delete(Set.of("booking:customer:1:list:1"));
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
        verify(bookingMapper, never()).updateCustomerCancelIfCancellable(
                anyLong(), anyLong(), anyString(), anyString(), anyString(), any(LocalDateTime.class));

    }

    @Test
    void specialistForceCancelBooking_ReleaseSlot_Success() {
        Long bookingId = 501L;
        Long specialistUserId = 2L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(9L);
        booking.setSpecialistId(20L);
        booking.setSlotId(99L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());

        TimeSlot slot = new TimeSlot();
        slot.setId(99L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.now());

        SpecialistForceCancelBookingRequestDTO requestDTO = new SpecialistForceCancelBookingRequestDTO();
        requestDTO.setCancelReason("Emergency issue");
        requestDTO.setReleaseSlot(true);

        when(bookingMapper.countBookingOwnedBySpecialist(bookingId, specialistUserId)).thenReturn(1L);
        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(99L)).thenReturn(slot);
        when(bookingMapper.updateSpecialistForceCancelIfCancellable(
                eq(bookingId),
                anyLong(),
                eq(BookingStatusEnum.CANCELLED.name()),
                eq("SPECIALIST_MANUAL"),
                eq("Emergency issue"),
                eq("Emergency issue"),
                eq("SPECIALIST_FORCE_CANCEL"),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);

        bookingService.specialistForceCancelBooking(bookingId, specialistUserId, requestDTO);

        verify(bookingMapper).updateSpecialistForceCancelIfCancellable(
                eq(bookingId),
                anyLong(),
                eq(BookingStatusEnum.CANCELLED.name()),
                eq("SPECIALIST_MANUAL"),
                eq("Emergency issue"),
                eq("Emergency issue"),
                eq("SPECIALIST_FORCE_CANCEL"),
                any(LocalDateTime.class)
        );
        verify(timeSlotMapper).update(argThat(updated ->
                TimeSlotStatusEnum.AVAILABLE.name().equals(updated.getStatus())
        ), any());
    }

    @Test
    void specialistForceCancelBooking_LockSlot_Success() {
        Long bookingId = 502L;
        Long specialistUserId = 2L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(9L);
        booking.setSpecialistId(21L);
        booking.setSlotId(100L);
        booking.setStatus(BookingStatusEnum.PENDING.name());

        TimeSlot slot = new TimeSlot();
        slot.setId(100L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.now());

        SpecialistForceCancelBookingRequestDTO requestDTO = new SpecialistForceCancelBookingRequestDTO();
        requestDTO.setCancelReason("Clinic unavailable");
        requestDTO.setReleaseSlot(false);

        when(bookingMapper.countBookingOwnedBySpecialist(bookingId, specialistUserId)).thenReturn(1L);
        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(100L)).thenReturn(slot);
        when(bookingMapper.updateSpecialistForceCancelIfCancellable(
                eq(bookingId),
                anyLong(),
                eq(BookingStatusEnum.CANCELLED.name()),
                eq("SPECIALIST_MANUAL"),
                eq("Clinic unavailable"),
                eq("Clinic unavailable"),
                eq("SPECIALIST_FORCE_CANCEL"),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);

        bookingService.specialistForceCancelBooking(bookingId, specialistUserId, requestDTO);

        verify(timeSlotMapper).update(argThat(updated ->
                TimeSlotStatusEnum.LOCKED.name().equals(updated.getStatus())
        ), any());
    }

    @Test
    void specialistForceCancelBooking_WithinTwoHours_ThrowsException() {
        Long bookingId = 503L;
        Long specialistUserId = 2L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setSlotId(101L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());

        LocalDateTime startAt = LocalDateTime.now().plusMinutes(90);
        TimeSlot slot = new TimeSlot();
        slot.setId(101L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        slot.setSlotDate(startAt.toLocalDate());
        slot.setStartTime(startAt.toLocalTime());

        SpecialistForceCancelBookingRequestDTO requestDTO = new SpecialistForceCancelBookingRequestDTO();
        requestDTO.setCancelReason("Emergency");
        requestDTO.setReleaseSlot(true);

        when(bookingMapper.countBookingOwnedBySpecialist(bookingId, specialistUserId)).thenReturn(1L);
        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(101L)).thenReturn(slot);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.specialistForceCancelBooking(bookingId, specialistUserId, requestDTO));
        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("Cannot cancel within 2 hours before start time", ex.getMessage());
        verify(bookingMapper, never()).updateSpecialistForceCancelIfCancellable(
                anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void customerCancellationConfirm_ConcurrentBookingUpdateConflict() {
        Long bookingId = 1300L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSlotId(188L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("120.00"));

        TimeSlot slot = new TimeSlot();
        slot.setId(188L);
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
        when(timeSlotMapper.selectById(188L)).thenReturn(slot);
        when(customerBookingChangePolicyService.customerCancellationQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class)))
                .thenReturn(quoted);
        when(bookingMapper.updateCustomerCancelIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(BookingStatusEnum.CANCELLED.name()),
                eq("CUSTOMER_MANUAL"),
                eq("CANCEL"),
                any(LocalDateTime.class)))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerCancellationConfirm(bookingId, 1L));
        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), ex.getCode());
        assertEquals("Booking status changed, please refresh and try again", ex.getMessage());
        verify(timeSlotMapper, never()).update(any(TimeSlot.class), any());
    }

    @Test
    void customerRescheduleConfirm_ConcurrentBookingUpdateConflict() {
        Long bookingId = 1402L;
        Long newSlotId = 179L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(1L);
        booking.setSpecialistId(5L);
        booking.setSlotId(156L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setPrice(new BigDecimal("100.00"));

        TimeSlot currentSlot = new TimeSlot();
        currentSlot.setId(156L);
        currentSlot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        currentSlot.setSlotDate(LocalDate.of(2026, 5, 1));
        currentSlot.setStartTime(LocalTime.of(14, 0));

        TimeSlot newSlot = new TimeSlot();
        newSlot.setId(newSlotId);
        newSlot.setSpecialistId(5L);
        newSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        newSlot.setSlotDate(LocalDate.now().plusDays(3));
        newSlot.setStartTime(LocalTime.of(10, 0));

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("120.00"));
        specialist.setStatus("ACTIVE");

        BookingRescheduleQuoteVO quoted = BookingRescheduleQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .build();

        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(156L)).thenReturn(currentSlot);
        when(timeSlotMapper.selectById(newSlotId)).thenReturn(newSlot);
        when(specialistQueryService.getSpecialistDetail(5L)).thenReturn(specialist);
        when(customerBookingChangePolicyService.customerRescheduleQuote(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(quoted);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1, 1);
        when(bookingMapper.updateCustomerRescheduleIfCancellable(
                eq(bookingId),
                eq(1L),
                eq(newSlotId),
                eq(BookingStatusEnum.PENDING.name()),
                eq("RESCHEDULE"),
                any(LocalDateTime.class)))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.customerRescheduleConfirm(bookingId, newSlotId, 1L));
        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), ex.getCode());
        assertEquals("Booking status changed, please refresh and try again", ex.getMessage());
    }

    @Test
    void specialistForceCancelBooking_ConcurrentBookingUpdateConflict() {
        Long bookingId = 1501L;
        Long specialistUserId = 2L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerId(9L);
        booking.setSpecialistId(12L);
        booking.setSlotId(199L);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());

        TimeSlot slot = new TimeSlot();
        slot.setId(199L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.now());

        SpecialistForceCancelBookingRequestDTO requestDTO = new SpecialistForceCancelBookingRequestDTO();
        requestDTO.setCancelReason("Emergency issue");
        requestDTO.setReleaseSlot(true);

        when(bookingMapper.countBookingOwnedBySpecialist(bookingId, specialistUserId)).thenReturn(1L);
        when(bookingMapper.selectById(bookingId)).thenReturn(booking);
        when(timeSlotMapper.selectById(199L)).thenReturn(slot);
        when(bookingMapper.updateSpecialistForceCancelIfCancellable(
                eq(bookingId),
                eq(12L),
                eq(BookingStatusEnum.CANCELLED.name()),
                eq("SPECIALIST_MANUAL"),
                eq("Emergency issue"),
                eq("Emergency issue"),
                eq("SPECIALIST_FORCE_CANCEL"),
                any(LocalDateTime.class)))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.specialistForceCancelBooking(bookingId, specialistUserId, requestDTO));
        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), ex.getCode());
        assertEquals("Booking status changed, please refresh and try again", ex.getMessage());
        verify(timeSlotMapper, never()).update(any(TimeSlot.class), any());
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
