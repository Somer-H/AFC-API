package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.CategoryEntity;
import com.afcgym.platform.service.dto.CreateCategoryCommand;
import java.util.List;

public interface ICategoryService {

    CategoryEntity create(CreateCategoryCommand command);

    List<CategoryEntity> list();
}
