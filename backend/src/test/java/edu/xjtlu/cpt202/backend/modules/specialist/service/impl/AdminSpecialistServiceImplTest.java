package edu.xjtlu.cpt202.backend.modules.specialist.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.AdminSpecialistMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.SpecialistFeeChangeRecordMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistCreateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.entity.SpecialistFeeChangeRecord;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.SpecialistFeeChangeRecordVO;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSpecialistServiceImplTest {

    @Mock
    private AdminSpecialistMapper adminSpecialistMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private SpecialistProfileMapper specialistProfileMapper;

    @Mock
    private SpecialistFeeChangeRecordMapper specialistFeeChangeRecordMapper;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Environment env;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSpecialistServiceImpl adminSpecialistService;

    @Test
    void listSpecialists_success_withDefaultPage() {
        AdminSpecialistListQueryDTO query = new AdminSpecialistListQueryDTO();
        query.setPageNo(null);
        query.setPageSize(null);

        AdminSpecialistListVO vo = new AdminSpecialistListVO();
        vo.setId(10001L);
        vo.setName("Test Specialist");
        vo.setConsultationFee(new BigDecimal("120.00"));
        vo.setStatus("Active");

        IPage<AdminSpecialistListVO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(1);
        mockPage.setRecords(Collections.singletonList(vo));

        when(adminSpecialistMapper.pageSpecialists(any(Page.class), eq(query))).thenReturn(mockPage);

        PageResult<AdminSpecialistListVO> result = adminSpecialistService.listSpecialists(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("Test Specialist", result.getList().get(0).getName());

        ArgumentCaptor<Page<AdminSpecialistListVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(adminSpecialistMapper).pageSpecialists(pageCaptor.capture(), eq(query));
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(10L, pageCaptor.getValue().getSize());
    }

    @Test
    void listSpecialists_success_withCustomPage() {
        AdminSpecialistListQueryDTO query = new AdminSpecialistListQueryDTO();
        query.setPageNo(2);
        query.setPageSize(5);

        IPage<AdminSpecialistListVO> mockPage = new Page<>(2, 5);
        mockPage.setTotal(0);
        mockPage.setRecords(Collections.emptyList());

        when(adminSpecialistMapper.pageSpecialists(any(Page.class), eq(query))).thenReturn(mockPage);

        PageResult<AdminSpecialistListVO> result = adminSpecialistService.listSpecialists(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getList().size());

        ArgumentCaptor<Page<AdminSpecialistListVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(adminSpecialistMapper).pageSpecialists(pageCaptor.capture(), eq(query));
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(5L, pageCaptor.getValue().getSize());
    }

    @Test
    void listSpecialists_emptyResult() {
        AdminSpecialistListQueryDTO query = new AdminSpecialistListQueryDTO();

        IPage<AdminSpecialistListVO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(0);
        mockPage.setRecords(Collections.emptyList());

        when(adminSpecialistMapper.pageSpecialists(any(Page.class), eq(query))).thenReturn(mockPage);

        PageResult<AdminSpecialistListVO> result = adminSpecialistService.listSpecialists(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getList().size());
    }

    @Test
    void updateSpecialist_allowsOutOfRangeFee() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);
        existing.setConsultationFee(new BigDecimal("260.00"));

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setEmail("emily.chen@example.com");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("300.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);
        when(adminSpecialistMapper.updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("300.00"), null, null, "ACTIVE"
        )).thenReturn(1);

        adminSpecialistService.updateSpecialist(1L, request);

        verify(adminSpecialistMapper).updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("300.00"), null, null, "ACTIVE"
        );
        verify(adminSpecialistMapper).updateUserAccountById(101L, "Dr. Emily Chen", "emily.chen@example.com", null);
        ArgumentCaptor<SpecialistFeeChangeRecord> recordCaptor = ArgumentCaptor.forClass(SpecialistFeeChangeRecord.class);
        verify(specialistFeeChangeRecordMapper).insert(recordCaptor.capture());
        assertEquals(new BigDecimal("260.00"), recordCaptor.getValue().getOldFee());
        assertEquals(new BigDecimal("300.00"), recordCaptor.getValue().getNewFee());
        assertEquals(Boolean.TRUE, recordCaptor.getValue().getOutOfRange());
    }

    @Test
    void updateSpecialist_success_whenFeeWithinRange() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setEmail("emily.chen@example.com");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("260.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);
        when(adminSpecialistMapper.updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, null, "ACTIVE"
        )).thenReturn(1);

        adminSpecialistService.updateSpecialist(1L, request);

        verify(adminSpecialistMapper).updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, null, "ACTIVE"
        );
        verify(adminSpecialistMapper).updateUserAccountById(101L, "Dr. Emily Chen", "emily.chen@example.com", null);
    }

    @Test
    void updateSpecialist_resetsPasswordToDefault_whenResetFlagTrue() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setEmail("emily.chen@example.com");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("260.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);
        request.setResetPasswordToDefault(true);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);
        when(adminSpecialistMapper.updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, null, "ACTIVE"
        )).thenReturn(1);
        when(passwordEncoder.encode("12345Expertlink")).thenReturn("encoded-default-password");

        adminSpecialistService.updateSpecialist(1L, request);

        verify(passwordEncoder).encode("12345Expertlink");
        verify(adminSpecialistMapper).updateUserAccountById(
                101L,
                "Dr. Emily Chen",
                "emily.chen@example.com",
                "encoded-default-password"
        );
        verify(mailSender, timeout(1000).atLeastOnce()).createMimeMessage();
    }

    @Test
    void updateSpecialist_doesNotResetDefaultPasswordOrSendEmail_whenResetFlagFalse() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setEmail("emily.chen@example.com");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("260.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);
        request.setResetPasswordToDefault(false);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);
        when(adminSpecialistMapper.updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, null, "ACTIVE"
        )).thenReturn(1);

        adminSpecialistService.updateSpecialist(1L, request);

        verify(passwordEncoder, never()).encode("12345Expertlink");
        verify(adminSpecialistMapper).updateUserAccountById(
                101L,
                "Dr. Emily Chen",
                "emily.chen@example.com",
                null
        );
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void updateSpecialist_ignoresCustomPassword_whenResetFlagTrue() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setEmail("emily.chen@example.com");
        request.setPassword("CustomPass123");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("260.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);
        request.setResetPasswordToDefault(true);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);
        when(adminSpecialistMapper.updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, null, "ACTIVE"
        )).thenReturn(1);
        when(passwordEncoder.encode("12345Expertlink")).thenReturn("encoded-default-password");

        adminSpecialistService.updateSpecialist(1L, request);

        verify(passwordEncoder).encode("12345Expertlink");
        verify(adminSpecialistMapper).updateUserAccountById(
                101L,
                "Dr. Emily Chen",
                "emily.chen@example.com",
                "encoded-default-password"
        );
    }

    @Test
    void createSpecialist_success_whenFeeWithinRange() {
        AdminSpecialistCreateDTO request = new AdminSpecialistCreateDTO();
        request.setName("Dr. New Specialist");
        request.setEmail("new.specialist@example.com");
        request.setCategoryId(1L);
        request.setLevel("SENIOR");
        request.setConsultationFee(new BigDecimal("220.00"));
        request.setStatus("Active");

        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        User createdUser = User.builder().id(99L).email("new.specialist@example.com").build();
        when(userAccountService.createUser("new.specialist@example.com", "12345Expertlink", "SPECIALIST", "Dr. New Specialist"))
                .thenReturn(createdUser);

        adminSpecialistService.createSpecialist(request);

        verify(userAccountService).createUser("new.specialist@example.com", "12345Expertlink", "SPECIALIST", "Dr. New Specialist");
        verify(specialistProfileMapper).insert(any());
        verify(mailSender, timeout(1000).atLeastOnce()).createMimeMessage();
    }

    @Test
    void createSpecialist_throwsBusinessException_whenFeeOutOfRange() {
        AdminSpecialistCreateDTO request = new AdminSpecialistCreateDTO();
        request.setName("Dr. New Specialist");
        request.setEmail("new.specialist@example.com");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("300.00"));
        request.setStatus("Active");

        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminSpecialistService.createSpecialist(request));

        assertEquals(400, exception.getCode());
        assertEquals("Consultation fee for CHIEF must be between 255.00 and 290.00", exception.getMessage());
        verify(userAccountService, never()).createUser(any(), any(), any(), any());
        verify(specialistProfileMapper, never()).insert(any());
    }

    @Test
    void getSpecialistDetail_success() {
        AdminSpecialistDetailVO detail = new AdminSpecialistDetailVO();
        detail.setId(1L);
        detail.setName("Dr. Emily Chen");

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(detail);

        AdminSpecialistDetailVO result = adminSpecialistService.getSpecialistDetail(1L);

        assertEquals(1L, result.getId());
        assertEquals("Dr. Emily Chen", result.getName());
    }

    @Test
    void getSpecialistDetail_throwsBusinessException_whenNotFound() {
        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminSpecialistService.getSpecialistDetail(1L));

        assertEquals(404, exception.getCode());
    }

    @Test
    void listFeeChangeRecords_success() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);

        SpecialistFeeChangeRecordVO record = new SpecialistFeeChangeRecordVO();
        record.setSpecialistId(1L);
        record.setLevel("CHIEF");

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(specialistFeeChangeRecordMapper.selectBySpecialistId(1L)).thenReturn(Collections.singletonList(record));

        List<SpecialistFeeChangeRecordVO> result = adminSpecialistService.listFeeChangeRecords(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSpecialistId());
        assertEquals("CHIEF", result.get(0).getLevel());
    }

    @Test
    void listFeeChangeRecords_throwsBusinessException_whenSpecialistNotFound() {
        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminSpecialistService.listFeeChangeRecords(1L));

        assertEquals(404, exception.getCode());
    }

    @Test
    void updateSpecialist_doesNotInsertFeeRecord_whenFeeUnchanged() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);
        existing.setConsultationFee(new BigDecimal("260.00"));

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setEmail("emily.chen@example.com");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("260.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);
        when(adminSpecialistMapper.updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, null, "ACTIVE"
        )).thenReturn(1);

        adminSpecialistService.updateSpecialist(1L, request);

        verify(adminSpecialistMapper).updateUserAccountById(101L, "Dr. Emily Chen", "emily.chen@example.com", null);
        verify(specialistFeeChangeRecordMapper, never()).insert(any());
    }

    @Test
    void updateSpecialistStatus_success() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);
        existing.setName("Dr. Emily Chen");

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.updateSpecialistStatusById(1L, "ACTIVE")).thenReturn(1);

        int cancelledBookingCount = adminSpecialistService.updateSpecialistStatus(1L, "Active");

        verify(adminSpecialistMapper).updateSpecialistStatusById(1L, "ACTIVE");
        assertEquals(0, cancelledBookingCount);
    }

    @Test
    void updateSpecialistStatus_returnsCancelledBookingCount_whenDeactivatingWithBookings() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);
        existing.setName("Dr. Emily Chen");

        Booking booking1 = new Booking();
        booking1.setId(101L);
        booking1.setSpecialistId(1L);
        booking1.setCustomerId(201L);
        booking1.setStatus(BookingStatusEnum.PENDING.name());

        Booking booking2 = new Booking();
        booking2.setId(102L);
        booking2.setSpecialistId(1L);
        booking2.setCustomerId(202L);
        booking2.setStatus(BookingStatusEnum.CONFIRMED.name());

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.updateSpecialistStatusById(1L, "INACTIVE")).thenReturn(1);
        when(bookingMapper.selectList(any())).thenReturn(List.of(booking1, booking2));

        int cancelledBookingCount = adminSpecialistService.updateSpecialistStatus(1L, "Inactive");

        assertEquals(2, cancelledBookingCount);
        assertEquals(BookingStatusEnum.CANCELLED.name(), booking1.getStatus());
        assertEquals(BookingStatusEnum.CANCELLED.name(), booking2.getStatus());
        verify(bookingMapper).updateById(booking1);
        verify(bookingMapper).updateById(booking2);
    }

    @Test
    void updateSpecialistStatus_throwsBusinessException_whenSpecialistNotFound() {
        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminSpecialistService.updateSpecialistStatus(1L, "Active"));

        assertEquals(404, exception.getCode());
    }
}
