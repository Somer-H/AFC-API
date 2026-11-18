package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.persistance.entities.CategoryEntity;
import com.afcgym.platform.persistance.repositories.ICategoryRepository;
import com.afcgym.platform.service.ICategoryService;
import com.afcgym.platform.service.dto.CreateCategoryCommand;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final ICategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryEntity create(CreateCategoryCommand command) {
        if (categoryRepository.existsByName(command.name())) {
            throw new ConflictException("Ya existe una categoría con ese nombre");
        }
        CategoryEntity category = CategoryEntity.builder()
                .name(command.name())
                .createdAt(Instant.now())
                .build();
        return categoryRepository.save(category);
    }

    @Override
    public List<CategoryEntity> list() {
        return categoryRepository.findAll();
    }
}
