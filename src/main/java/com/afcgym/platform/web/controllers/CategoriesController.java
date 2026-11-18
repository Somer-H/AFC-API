package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.CategoryMapper;
import com.afcgym.platform.service.ICategoryService;
import com.afcgym.platform.service.dto.CreateCategoryCommand;
import com.afcgym.platform.web.dto.request.CreateCategoryRequest;
import com.afcgym.platform.web.dto.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoriesController {

    private final ICategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product category")
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryMapper.toResponse(categoryService.create(new CreateCategoryCommand(request.name())));
    }

    @GetMapping
    @Operation(summary = "List product categories")
    public List<CategoryResponse> list() {
        return categoryService.list().stream().map(categoryMapper::toResponse).toList();
    }
}
