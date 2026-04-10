package edu.xjtlu.cpt202.backend.modules.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.modules.category.model.dto.CategoryRequest;
import edu.xjtlu.cpt202.backend.modules.category.service.ExpertiseCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCategoryControllerTest {

    private MockMvc mockMvc;
    private ExpertiseCategoryService expertiseCategoryService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        expertiseCategoryService = Mockito.mock(ExpertiseCategoryService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminCategoryController(expertiseCategoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createCategory_rejectsSpecialCharacters() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Cardiology@123");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Category name can only contain letters and spaces"));

        verifyNoInteractions(expertiseCategoryService);
    }

    @Test
    void createCategory_rejectsChineseCharacters() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("心脏科");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Category name can only contain letters and spaces"));

        verifyNoInteractions(expertiseCategoryService);
    }

    @Test
    void createCategory_rejectsNumbers() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Cardiology123");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Category name can only contain letters and spaces"));

        verifyNoInteractions(expertiseCategoryService);
    }

    @Test
    void createCategory_rejectsTooLongName() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXY");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Category name must be at most 50 characters"));

        verifyNoInteractions(expertiseCategoryService);
    }
}
