package edu.xjtlu.cpt202.backend.modules.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.category.entity.ExpertiseCategory;
import edu.xjtlu.cpt202.backend.modules.category.mapper.ExpertiseCategoryMapper;
import edu.xjtlu.cpt202.backend.modules.category.model.dto.CategoryRequest;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpertiseCategoryServiceImplTest {

    @Mock
    private ExpertiseCategoryMapper expertiseCategoryMapper;

    @Mock
    private SpecialistProfileMapper specialistProfileMapper;

    @InjectMocks
    private ExpertiseCategoryServiceImpl expertiseCategoryService;

    @Test
    void createCategory_success() {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("  Cardiology  ");

        when(expertiseCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        expertiseCategoryService.createCategory(request);

        ArgumentCaptor<ExpertiseCategory> categoryCaptor = ArgumentCaptor.forClass(ExpertiseCategory.class);
        verify(expertiseCategoryMapper).insert(categoryCaptor.capture());
        assertEquals("Cardiology", categoryCaptor.getValue().getCategoryName());
    }

    @Test
    void createCategory_throwsBusinessException_whenNameAlreadyExists() {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Cardiology");

        when(expertiseCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> expertiseCategoryService.createCategory(request));

        assertEquals(400, exception.getCode());
        assertEquals("Category name already exists", exception.getMessage());
        verify(expertiseCategoryMapper, never()).insert(any(ExpertiseCategory.class));
    }

    @Test
    void updateCategory_success_whenNameUnique() {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("  Psychology  ");

        ExpertiseCategory existing = new ExpertiseCategory();
        existing.setId(1L);
        existing.setCategoryName("Cardiology");

        when(expertiseCategoryMapper.selectById(1L)).thenReturn(existing);
        when(expertiseCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        expertiseCategoryService.updateCategory(1L, request);

        ArgumentCaptor<ExpertiseCategory> categoryCaptor = ArgumentCaptor.forClass(ExpertiseCategory.class);
        verify(expertiseCategoryMapper).updateById(categoryCaptor.capture());
        assertEquals(1L, categoryCaptor.getValue().getId());
        assertEquals("Psychology", categoryCaptor.getValue().getCategoryName());
    }

    @Test
    void updateCategory_throwsBusinessException_whenNameAlreadyExists() {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Psychology");

        ExpertiseCategory existing = new ExpertiseCategory();
        existing.setId(1L);
        existing.setCategoryName("Cardiology");

        when(expertiseCategoryMapper.selectById(1L)).thenReturn(existing);
        when(expertiseCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> expertiseCategoryService.updateCategory(1L, request));

        assertEquals(400, exception.getCode());
        assertEquals("Category name already exists", exception.getMessage());
        verify(expertiseCategoryMapper, never()).updateById(any(ExpertiseCategory.class));
    }

    @Test
    void deleteCategory_throwsBusinessException_whenCategoryAssignedToSpecialists() {
        ExpertiseCategory category = new ExpertiseCategory();
        category.setId(1L);
        category.setCategoryName("Cardiology");

        when(expertiseCategoryMapper.selectById(1L)).thenReturn(category);
        when(specialistProfileMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> expertiseCategoryService.deleteCategory(1L));

        assertEquals(400, exception.getCode());
        assertEquals("Category is already assigned to specialists and cannot be deleted", exception.getMessage());
        verify(expertiseCategoryMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteCategory_deletesCategory_whenUnused() {
        ExpertiseCategory category = new ExpertiseCategory();
        category.setId(1L);
        category.setCategoryName("Cardiology");

        when(expertiseCategoryMapper.selectById(1L)).thenReturn(category);
        when(specialistProfileMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        expertiseCategoryService.deleteCategory(1L);

        verify(expertiseCategoryMapper).deleteById(1L);
    }
}
