package edu.xjtlu.cpt202.backend.common.security;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import edu.xjtlu.cpt202.backend.common.config.JwtConfig;
import edu.xjtlu.cpt202.backend.common.config.SecurityConfig;
import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import edu.xjtlu.cpt202.backend.modules.booking.controller.BookingController;
import edu.xjtlu.cpt202.backend.modules.booking.controller.BookingTopicController;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingTopicService;
import edu.xjtlu.cpt202.backend.modules.category.controller.AdminCategoryController;
import edu.xjtlu.cpt202.backend.modules.category.model.vo.CategoryVO;
import edu.xjtlu.cpt202.backend.modules.category.service.ExpertiseCategoryService;
import edu.xjtlu.cpt202.backend.modules.schedule.controller.ScheduleController;
import edu.xjtlu.cpt202.backend.modules.schedule.controller.SpecialistQueryController;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.ScheduleService;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author QiranXiao
 * @since 2026/4/17
 *
 */
@SpringBootTest(classes = SecurityAuthorizationIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private ExpertiseCategoryService expertiseCategoryService;

    @MockBean
    private SpecialistQueryService specialistQueryService;

    @MockBean
    private BookingTopicService bookingTopicService;

    @BeforeEach
    void setUp() {
        when(userMapper.selectById(1001L)).thenReturn(activeUser(1001L, "CUSTOMER"));
        when(userMapper.selectById(1002L)).thenReturn(activeUser(1002L, "SPECIALIST"));
        when(userMapper.selectById(1003L)).thenReturn(activeUser(1003L, "ADMIN"));
        when(bookingService.getUpcomingBookingsByCustomer(anyLong(), anyInt())).thenReturn(List.of(new UpcomingBookingVO()));
        when(scheduleService.getWeeklySchedule(any(LocalDate.class))).thenReturn(List.of(new TimeSlotVO()));
        when(expertiseCategoryService.listCategories()).thenReturn(List.of(new CategoryVO()));
        when(specialistQueryService.searchSpecialists(any())).thenReturn(new PageResult<SpecialistSummaryVO>(0, List.of()));
        when(specialistQueryService.listCategories()).thenReturn(List.of());
        when(bookingTopicService.listActiveTopicNames()).thenReturn(List.of("Initial Consultation"));
    }

    @Test
    void customerCanAccessCustomerApisButForbiddenForSpecialistAndAdminApis() throws Exception {
        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                        .header(HttpHeaders.AUTHORIZATION, bearer("CUSTOMER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/specialist/schedule/slots/weekly")
                        .header(HttpHeaders.AUTHORIZATION, bearer("CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void specialistCanAccessSpecialistApisButForbiddenForCustomerAndAdminApis() throws Exception {
        mockMvc.perform(get("/api/specialist/schedule/slots/weekly")
                        .header(HttpHeaders.AUTHORIZATION, bearer("SPECIALIST")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                        .header(HttpHeaders.AUTHORIZATION, bearer("SPECIALIST")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer("SPECIALIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminApisButForbiddenForCustomerAndSpecialistApis() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/specialist/schedule/slots/weekly")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestsShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/specialist/schedule/slots/weekly"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void queryApisShouldRequireAuthenticationButNotSpecificRole() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/specialists"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/booking-topics")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/booking-topics"))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(String role) {
        return "Bearer " + JwtUtils.generateToken(userIdForRole(role), role);
    }

    private Long userIdForRole(String role) {
        return switch (role) {
            case "CUSTOMER" -> 1001L;
            case "SPECIALIST" -> 1002L;
            case "ADMIN" -> 1003L;
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
    }

    private User activeUser(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(AccountStatusEnum.ACTIVE.name());
        return user;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            MybatisPlusAutoConfiguration.class
    })
    @Import({
            JwtConfig.class,
            SecurityConfig.class,
            GlobalExceptionHandler.class,
            BookingController.class,
            ScheduleController.class,
            AdminCategoryController.class,
            SpecialistQueryController.class,
            BookingTopicController.class
    })
    static class TestApplication {
    }
}
