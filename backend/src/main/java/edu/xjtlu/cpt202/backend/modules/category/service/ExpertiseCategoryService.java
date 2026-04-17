package edu.xjtlu.cpt202.backend.modules.category.service;

import edu.xjtlu.cpt202.backend.modules.category.model.dto.CategoryRequest;
import edu.xjtlu.cpt202.backend.modules.category.model.vo.CategoryVO;

import java.util.List;

public interface ExpertiseCategoryService {

    List<CategoryVO> listCategories();

    void createCategory(CategoryRequest request);

    void updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
