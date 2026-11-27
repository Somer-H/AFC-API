package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.InventoryMovementEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.persistance.entities.enums.MovementType;
import com.afcgym.platform.persistance.repositories.ICategoryRepository;
import com.afcgym.platform.persistance.repositories.IInventoryMovementRepository;
import com.afcgym.platform.persistance.repositories.IProductRepository;
import com.afcgym.platform.persistance.repositories.ISupplierRepository;
import com.afcgym.platform.persistance.repositories.specifications.ProductSpecifications;
import com.afcgym.platform.service.IProductService;
import com.afcgym.platform.service.dto.CreateProductCommand;
import com.afcgym.platform.service.dto.RegisterPurchaseCommand;
import com.afcgym.platform.service.dto.UpdateProductCommand;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private static final int DEFAULT_MIN_STOCK = 5;

    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final ISupplierRepository supplierRepository;
    private final IInventoryMovementRepository inventoryMovementRepository;

    @Override
    @Transactional
    public ProductEntity create(CreateProductCommand command) {
        if (productRepository.existsByCode(command.code())) {
            throw new ConflictException("Ya existe un producto con ese código");
        }
        validateCategoryExists(command.categoryId());
        validateSupplierExists(command.supplierId());
        ProductEntity product = ProductEntity.builder()
                .code(command.code())
                .name(command.name())
                .categoryId(command.categoryId())
                .supplierId(command.supplierId())
                .price(command.price())
                .cost(command.cost())
                .stock(command.stock() != null ? command.stock() : 0)
                .minStock(command.minStock() != null ? command.minStock() : DEFAULT_MIN_STOCK)
                .active(true)
                .createdAt(Instant.now())
                .build();
        return productRepository.save(product);
    }

    @Override
    public ProductEntity getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se encontró el producto solicitado"));
    }

    @Override
    public List<ProductEntity> search(String search, Long categoryId, Boolean lowStock) {
        Specification<ProductEntity> specification = (search == null || search.isBlank())
                ? ProductSpecifications.all()
                : ProductSpecifications.searchMatches(search);
        if (categoryId != null) {
            specification = specification.and(ProductSpecifications.inCategory(categoryId));
        }
        if (Boolean.TRUE.equals(lowStock)) {
            specification = specification.and(ProductSpecifications.lowStock());
        }
        return productRepository.findAll(specification);
    }

    @Override
    @Transactional
    public ProductEntity update(Long id, UpdateProductCommand command) {
        ProductEntity product = getById(id);
        validateCategoryExists(command.categoryId());
        validateSupplierExists(command.supplierId());
        product.updateDetails(
                command.name(), command.categoryId(), command.supplierId(),
                command.price(), command.cost(), command.minStock());
        return product;
    }

    @Override
    @Transactional
    public ProductEntity deactivate(Long id) {
        ProductEntity product = getById(id);
        product.deactivate();
        return product;
    }

    @Override
    @Transactional
    public InventoryMovementEntity registerPurchase(Long productId, RegisterPurchaseCommand command) {
        if (command.quantity() <= 0) {
            throw new BadRequestException("La cantidad debe ser mayor a cero");
        }
        ProductEntity product = getById(productId);
        product.increaseStock(command.quantity());
        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .productId(product.getId())
                .type(MovementType.PURCHASE)
                .quantity(command.quantity())
                .unitCost(command.unitCost())
                .finalStock(product.getStock())
                .userId(command.userId())
                .createdAt(Instant.now())
                .build();
        return inventoryMovementRepository.save(movement);
    }

    @Override
    public List<InventoryMovementEntity> listMovements(Long productId) {
        getById(productId);
        return inventoryMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    private void validateCategoryExists(Long categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("No se encontró la categoría solicitada");
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierId != null && !supplierRepository.existsById(supplierId)) {
            throw new NotFoundException("No se encontró el proveedor solicitado");
        }
    }
}
