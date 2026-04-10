package edu.xjtlu.cpt202.backend.modules.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.category.entity.ExpertiseCategory;
import edu.xjtlu.cpt202.backend.modules.category.mapper.ExpertiseCategoryMapper;
import edu.xjtlu.cpt202.backend.modules.category.model.dto.CategoryRequest;
import edu.xjtlu.cpt202.backend.modules.category.model.vo.CategoryVO;
import edu.xjtlu.cpt202.backend.modules.category.service.ExpertiseCategoryService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.SpecialistProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExpertiseCategoryServiceImpl implements ExpertiseCategoryService {

    private final ExpertiseCategoryMapper expertiseCategoryMapper;
    private final SpecialistProfileMapper specialistProfileMapper;

    public ExpertiseCategoryServiceImpl(
            ExpertiseCategoryMapper expertiseCategoryMapper,
            SpecialistProfileMapper specialistProfileMapper
    ) {
        this.expertiseCategoryMapper = expertiseCategoryMapper;
        this.specialistProfileMapper = specialistProfileMapper;
    }

    @Override
    public List<CategoryVO> listCategories() {
        List<ExpertiseCategory> categories = expertiseCategoryMapper.selectList(
                new LambdaQueryWrapper<ExpertiseCategory>()
                        .orderByAsc(ExpertiseCategory::getId)
        );

        return categories.stream().map(this::toCategoryVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(CategoryRequest request) {
        String categoryName = normalizeCategoryName(request.getCategoryName());

        ensureCategoryNameUnique(categoryName, null);

        ExpertiseCategory category = new ExpertiseCategory();
        category.setCategoryName(categoryName);
        expertiseCategoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryRequest request) {
        ExpertiseCategory existingCategory = getCategoryOrThrow(id);
        String categoryName = normalizeCategoryName(request.getCategoryName());

        ensureCategoryNameUnique(categoryName, id);

        existingCategory.setCategoryName(categoryName);
        expertiseCategoryMapper.updateById(existingCategory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        ExpertiseCategory existingCategory = getCategoryOrThrow(id);
        ensureCategoryNotUsed(existingCategory.getId());
        expertiseCategoryMapper.deleteById(existingCategory.getId());
    }

    private ExpertiseCategory getCategoryOrThrow(Long id) {
        ExpertiseCategory category = expertiseCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "Category not found");
        }
        return category;
    }

    private void ensureCategoryNameUnique(String categoryName, Long excludeId) {
        LambdaQueryWrapper<ExpertiseCategory> queryWrapper = new LambdaQueryWrapper<ExpertiseCategory>()
                .eq(ExpertiseCategory::getCategoryName, categoryName);

        if (excludeId != null) {
            queryWrapper.ne(ExpertiseCategory::getId, excludeId);
        }

        Long count = expertiseCategoryMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Category name already exists");
        }
    }

    private String normalizeCategoryName(String categoryName) {
        return categoryName == null ? "" : categoryName.trim();
    }

    private void ensureCategoryNotUsed(Long categoryId) {
        Long specialistCount = specialistProfileMapper.selectCount(
                new LambdaQueryWrapper<SpecialistProfile>()
                        .eq(SpecialistProfile::getCategoryId, categoryId)
        );
        if (specialistCount != null && specialistCount > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(),
                    "Category is already assigned to specialists and cannot be deleted");
        }
    }

    private CategoryVO toCategoryVO(ExpertiseCategory category) {
        CategoryVO categoryVO = new CategoryVO();
        categoryVO.setId(category.getId());
        categoryVO.setCategoryName(category.getCategoryName());
        categoryVO.setCreateTime(category.getCreatedAt());
        return categoryVO;
    }
}
