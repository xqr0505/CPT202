package edu.xjtlu.cpt202.backend.modules.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.modules.category.model.dto.CategoryRequest;
import edu.xjtlu.cpt202.backend.modules.category.model.vo.CategoryVO;
import edu.xjtlu.cpt202.backend.modules.category.service.ExpertiseCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void listCategories_success() throws Exception {
        CategoryVO category = new CategoryVO();
        category.setId(1L);
        category.setCategoryName("Cardiology");
        category.setCreateTime(LocalDateTime.of(2026, 4, 1, 10, 30, 0));
        Mockito.when(expertiseCategoryService.listCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").value("Cardiology"))
                .andExpect(jsonPath("$.data[0].createTime").value("2026-04-01 10:30:00"));

        verify(expertiseCategoryService).listCategories();
    }

    @Test
    void createCategory_success() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Cardiology");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"));

        ArgumentCaptor<CategoryRequest> captor = ArgumentCaptor.forClass(CategoryRequest.class);
        verify(expertiseCategoryService).createCategory(captor.capture());
        assertEquals("Cardiology", captor.getValue().getCategoryName());
    }

    @Test
    void createCategory_rejectsBlankName() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("   ");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Category name cannot be empty"));

        verifyNoInteractions(expertiseCategoryService);
    }

    @Test
    void createCategory_rejectsSpecialCharacters() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Cardiology@123");

        mockMvc.perform(post("/api/v1/admin/categories")
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
        request.setCategoryName("\u5fc3\u810f\u79d1");

        mockMvc.perform(post("/api/v1/admin/categories")
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

        mockMvc.perform(post("/api/v1/admin/categories")
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

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Category name must be at most 50 characters"));

        verifyNoInteractions(expertiseCategoryService);
    }

    @Test
    void updateCategory_success() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Neurology");

        mockMvc.perform(put("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"));

        ArgumentCaptor<CategoryRequest> captor = ArgumentCaptor.forClass(CategoryRequest.class);
        verify(expertiseCategoryService).updateCategory(Mockito.eq(1L), captor.capture());
        assertEquals("Neurology", captor.getValue().getCategoryName());
    }

    @Test
    void updateCategory_rejectsInvalidName() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Neuro123");

        mockMvc.perform(put("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Category name can only contain letters and spaces"));

        verifyNoInteractions(expertiseCategoryService);
    }

    @Test
    void updateCategory_rejectsInvalidIdType() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Neurology");

        mockMvc.perform(put("/api/v1/admin/categories/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid parameters"));

        verifyNoInteractions(expertiseCategoryService);
    }

    @Test
    void deleteCategory_success() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"));

        verify(expertiseCategoryService).deleteCategory(1L);
    }

    @Test
    void deleteCategory_rejectsInvalidIdType() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/categories/abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid parameters"));

        verifyNoInteractions(expertiseCategoryService);
    }
}
