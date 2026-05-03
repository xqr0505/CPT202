package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.ai.model.CancelTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelTaskStateStore;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowAssistant;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.DashboardQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistForceCancelBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistRejectBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.UsageSummaryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardStatisticsVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UsageSummaryVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
/**
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
class CancelWorkflowServiceImplTest {

    @Test
    void shouldStartWorkflowForCancelIntent() {
        CancelWorkflowServiceImpl service = new CancelWorkflowServiceImpl(
                new InMemoryCancelTaskStateStore(),
                (userMsg, taskState) -> "continue",
                new StubBookingService(),
                (memoryId, userMessage) -> AiIntent.CANCEL
        );

        boolean shouldStart = service.shouldStartWorkflow(1001L, "cancel my booking");

        assertThat(shouldStart).isTrue();
    }

    @Test
    void shouldReturnCandidateListWhenBookingIsAmbiguous() {
        InMemoryCancelTaskStateStore store = new InMemoryCancelTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        bookingService.bookingList = List.of(
                booking("11", "Dr. Smith", "Therapy", LocalDateTime.of(2026, 5, 10, 9, 0), "CONFIRMED"),
                booking("12", "Dr. Lee", "Consultation", LocalDateTime.of(2026, 5, 11, 14, 0), "PENDING")
        );
        CancelWorkflowServiceImpl service = new CancelWorkflowServiceImpl(
                store,
                (userMsg, taskState) -> "continue",
                bookingService,
                (memoryId, userMessage) -> AiIntent.CANCEL
        );

        String reply = service.handle(1001L, wrapped("cancel my booking"));

        assertThat(reply).contains("Please reply with the booking ID");
        assertThat(reply).contains("ID 11");
        assertThat(reply).contains("ID 12");
        assertThat(store.get(1001L)).isPresent();
        assertThat(store.get(1001L).get().getStep()).isEqualTo(CancelTaskState.Step.IDENTIFY);
    }

    @Test
    void shouldValidateImmediatelyWhenBookingIdIsExplicit() {
        InMemoryCancelTaskStateStore store = new InMemoryCancelTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        bookingService.bookingList = List.of(
                booking("15", "Dr. Smith", "Therapy", LocalDateTime.of(2026, 5, 10, 9, 0), "CONFIRMED")
        );
        bookingService.quote = BookingCancelQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .bookingStartAt(LocalDateTime.of(2026, 5, 10, 9, 0))
                .refundAmount(new BigDecimal("100.00"))
                .penaltyAmount(BigDecimal.ZERO)
                .build();
        CancelWorkflowServiceImpl service = new CancelWorkflowServiceImpl(
                store,
                (userMsg, taskState) -> "continue",
                bookingService,
                (memoryId, userMessage) -> AiIntent.CANCEL
        );

        String reply = service.handle(1001L, wrapped("cancel booking 15"));

        assertThat(reply).contains("[TRIGGER_CANCEL_MODAL:15]");
        assertThat(reply).contains("Please review the cancellation details and confirm manually.");
        assertThat(store.get(1001L)).isEmpty();
    }

    @Test
    void shouldAbortAndClearStateWhenAssistantReturnsAbortMarker() {
        InMemoryCancelTaskStateStore store = new InMemoryCancelTaskStateStore();
        store.save(1001L, CancelTaskState.builder().step(CancelTaskState.Step.IDENTIFY).build());
        CancelWorkflowServiceImpl service = new CancelWorkflowServiceImpl(
                store,
                (userMsg, taskState) -> "[CANCEL_TASK_ABORTED] unrelated topic",
                new StubBookingService(),
                (memoryId, userMessage) -> AiIntent.CANCEL
        );

        String reply = service.handle(1001L, wrapped("what time is it?"));

        assertThat(reply).startsWith("[CANCEL_TASK_ABORTED]");
        assertThat(store.get(1001L)).isEmpty();
    }

    @Test
    void shouldAbortAndClearStateForShortcutPhrase() {
        InMemoryCancelTaskStateStore store = new InMemoryCancelTaskStateStore();
        store.save(1001L, CancelTaskState.builder().step(CancelTaskState.Step.IDENTIFY).build());
        CancelWorkflowServiceImpl service = new CancelWorkflowServiceImpl(
                store,
                (userMsg, taskState) -> "continue",
                new StubBookingService(),
                (memoryId, userMessage) -> AiIntent.CANCEL
        );

        String reply = service.handle(1001L, wrapped("never mind"));

        assertThat(reply).startsWith("[CANCEL_TASK_ABORTED]");
        assertThat(store.get(1001L)).isEmpty();
    }

    @Test
    void shouldReturnDisallowedReasonAndClearState() {
        InMemoryCancelTaskStateStore store = new InMemoryCancelTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        bookingService.bookingList = List.of(
                booking("15", "Dr. Smith", "Therapy", LocalDateTime.of(2026, 5, 10, 9, 0), "CONFIRMED")
        );
        bookingService.quote = BookingCancelQuoteVO.builder()
                .allowed(false)
                .message("Less than 2 hours to start")
                .build();
        CancelWorkflowServiceImpl service = new CancelWorkflowServiceImpl(
                store,
                (userMsg, taskState) -> "continue",
                bookingService,
                (memoryId, userMessage) -> AiIntent.CANCEL
        );

        String reply = service.handle(1001L, wrapped("cancel booking 15"));

        assertThat(reply).contains("cannot be cancelled");
        assertThat(reply).contains("Less than 2 hours to start");
        assertThat(store.get(1001L)).isEmpty();
    }

    private static BookingItemVO booking(
            String id,
            String specialist,
            String service,
            LocalDateTime time,
            String status
    ) {
        return BookingItemVO.builder()
                .id(id)
                .specialistName(specialist)
                .serviceName(service)
                .appointmentDateTime(time)
                .status(status)
                .build();
    }

    private static String wrapped(String userMessage) {
        return """
                Current system time: 2026-05-04 10:00:00 CST
                Use this as the authoritative current time when interpreting relative dates such as today, tomorrow, upcoming, this week, and history.

                User message:
                %s
                """.formatted(userMessage);
    }

    private static class InMemoryCancelTaskStateStore implements CancelTaskStateStore {

        private final Map<Long, CancelTaskState> states = new HashMap<>();

        @Override
        public Optional<CancelTaskState> get(Long userId) {
            return Optional.ofNullable(states.get(userId));
        }

        @Override
        public void save(Long userId, CancelTaskState state) {
            states.put(userId, state);
        }

        @Override
        public void clear(Long userId) {
            states.remove(userId);
        }
    }

    private static class StubBookingService implements BookingService {

        private List<BookingItemVO> bookingList = List.of();
        private BookingCancelQuoteVO quote = BookingCancelQuoteVO.builder().allowed(true).build();

        @Override
        public List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int autoCompleteExpiredConfirmedBookings() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingCreateVO createBooking(Long customerId, BookingCreateDTO createDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiBookingFormDraftVO buildAiBookingDraft(Long customerId, Long specialistId, Long slotId, String preferredTopic, String customerNotes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<BookingItemVO> getBookingList(Long customerId, BookingPageQueryDTO dto) {
            return new PageResult<>(bookingList.size(), bookingList);
        }

        @Override
        public UsageSummaryVO getUsageSummary(UsageSummaryQueryDTO queryDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DashboardStatisticsVO getDashboardStatistics(DashboardQueryDTO queryDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingDetailVO getBookingDetailById(Long bookingId, Long currentCustomerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingCancelQuoteVO customerCancellationQuote(Long bookingId, Long currentCustomerId) {
            return quote;
        }

        @Override
        public BookingCancelConfirmVO customerCancellationConfirm(Long bookingId, Long currentCustomerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingRescheduleQuoteVO customerRescheduleQuote(Long bookingId, Long newSlotId, Long currentCustomerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingRescheduleConfirmVO customerRescheduleConfirm(Long bookingId, Long newSlotId, Long currentCustomerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpecialistPendingBookingVO> listPendingRequestsForSpecialist(Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpecialistHandledBookingVO> listHandledRequestsForSpecialist(Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpecialistBookingDetailVO getBookingRequestDetailForSpecialist(Long bookingId, Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void approveBookingRequest(Long bookingId, Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rejectBookingRequest(Long bookingId, Long currentUserId, SpecialistRejectBookingRequestDTO requestDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void specialistForceCancelBooking(Long bookingId, Long currentUserId, SpecialistForceCancelBookingRequestDTO requestDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void systemTimeoutCancelPendingBooking(Long bookingId, String cancelReason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean save(Booking entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveBatch(java.util.Collection<Booking> entityList, int batchSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveOrUpdateBatch(java.util.Collection<Booking> entityList, int batchSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateBatchById(java.util.Collection<Booking> entityList, int batchSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveOrUpdate(Booking entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Booking getById(java.io.Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Booking> listByIds(java.util.Collection<? extends java.io.Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Booking> listByMap(Map<String, Object> columnMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Booking getOne(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper, boolean throwEx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Booking> getOneOpt(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper, boolean throwEx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Map<String, Object> getMap(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> V getObj(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper, java.util.function.Function<? super Object, V> mapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Booking> list(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Booking> list() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E extends com.baomidou.mybatisplus.core.metadata.IPage<Booking>> E page(E page, com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E extends com.baomidou.mybatisplus.core.metadata.IPage<Booking>> E page(E page) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> listMaps(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> listMaps() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> java.util.List<V> listObjs(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper, java.util.function.Function<? super Object, V> mapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Object> listObjs() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeById(java.io.Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeByMap(Map<String, Object> columnMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeByIds(java.util.Collection<?> list) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateById(Booking entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean update(com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean update(Booking entity, com.baomidou.mybatisplus.core.conditions.Wrapper<Booking> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveBatch(java.util.Collection<Booking> entityList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveOrUpdateBatch(java.util.Collection<Booking> entityList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateBatchById(java.util.Collection<Booking> entityList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.baomidou.mybatisplus.core.mapper.BaseMapper<Booking> getBaseMapper() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<Booking> getEntityClass() {
            return Booking.class;
        }
    }
}
