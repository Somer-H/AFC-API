package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.persistance.entities.CategoryEntity;
import com.afcgym.platform.persistance.repositories.ICategoryRepository;
import com.afcgym.platform.service.dto.CreateCategoryCommand;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private ICategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void givenANewName_whenCreate_thenSaveAndReturnTheCategory() {
        CreateCategoryCommand command = new CreateCategoryCommand("Suplementos");
        CategoryEntity saved = CategoryEntity.builder().id(1L).name("Suplementos").createdAt(Instant.now()).build();
        when(categoryRepository.existsByName("Suplementos")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(saved);

        CategoryEntity result = categoryService.create(command);

        assertEquals(saved, result);
    }

    @Test
    void givenAnExistingName_whenCreate_thenThrowConflictException() {
        when(categoryRepository.existsByName("Suplementos")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> categoryService.create(new CreateCategoryCommand("Suplementos")));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void whenList_thenReturnAllCategories() {
        CategoryEntity category = CategoryEntity.builder().id(1L).name("Suplementos").createdAt(Instant.now()).build();
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryEntity> result = categoryService.list();

        assertEquals(1, result.size());
    }
}
