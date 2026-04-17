package edu.xjtlu.cpt202.backend.modules.category.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.category.model.dto.CategoryRequest;
import edu.xjtlu.cpt202.backend.modules.category.model.vo.CategoryVO;
import edu.xjtlu.cpt202.backend.modules.category.service.ExpertiseCategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final ExpertiseCategoryService expertiseCategoryService;

    public AdminCategoryController(ExpertiseCategoryService expertiseCategoryService) {
        this.expertiseCategoryService = expertiseCategoryService;
    }

    @GetMapping
    public Result<List<CategoryVO>> listCategories() {
        return Result.success(expertiseCategoryService.listCategories());
    }

    @PostMapping
    public Result<Void> createCategory(@Valid @RequestBody CategoryRequest request) {
        expertiseCategoryService.createCategory(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        expertiseCategoryService.updateCategory(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        expertiseCategoryService.deleteCategory(id);
        return Result.success();
    }
}
