package edu.xjtlu.cpt202.backend.common.security;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import edu.xjtlu.cpt202.backend.common.config.SecurityConfig;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                        .with(authentication(auth("CUSTOMER"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/specialist/schedule/slots/weekly")
                        .with(authentication(auth("CUSTOMER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/categories")
                        .with(authentication(auth("CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void specialistCanAccessSpecialistApisButForbiddenForCustomerAndAdminApis() throws Exception {
        mockMvc.perform(get("/api/specialist/schedule/slots/weekly")
                        .with(authentication(auth("SPECIALIST"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                        .with(authentication(auth("SPECIALIST"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/categories")
                        .with(authentication(auth("SPECIALIST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminApisButForbiddenForCustomerAndSpecialistApis() throws Exception {
        mockMvc.perform(get("/admin/categories")
                        .with(authentication(auth("ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                        .with(authentication(auth("ADMIN"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/specialist/schedule/slots/weekly")
                        .with(authentication(auth("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestsShouldReturn401() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/specialist/schedule/slots/weekly"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void queryApisShouldRequireAuthenticationButNotSpecificRole() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .with(authentication(auth("CUSTOMER"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/specialists")
                        .with(authentication(auth("SPECIALIST"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/booking-topics")
                        .with(authentication(auth("ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/specialists"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/booking-topics"))
                .andExpect(status().isUnauthorized());
    }

    private Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                1001L,
                null,
                AuthorityUtils.createAuthorityList("ROLE_" + role)
        );
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
