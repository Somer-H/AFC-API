package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.InventoryMovementEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.service.dto.CreateProductCommand;
import com.afcgym.platform.service.dto.RegisterPurchaseCommand;
import com.afcgym.platform.service.dto.UpdateProductCommand;
import java.util.List;

public interface IProductService {

    ProductEntity create(CreateProductCommand command);

    ProductEntity getById(Long id);

    List<ProductEntity> search(String search, Long categoryId, Boolean lowStock);

    ProductEntity update(Long id, UpdateProductCommand command);

    ProductEntity deactivate(Long id);

    InventoryMovementEntity registerPurchase(Long productId, RegisterPurchaseCommand command);

    List<InventoryMovementEntity> listMovements(Long productId);
}
