package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.afcgym.platform.service.dto.CreateProductCommand;
import com.afcgym.platform.service.dto.RegisterPurchaseCommand;
import com.afcgym.platform.service.dto.UpdateProductCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private ISupplierRepository supplierRepository;

    @Mock
    private IInventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductEntity existingProduct;

    @BeforeEach
    void init() {
        existingProduct = ProductEntity.builder()
                .id(1L)
                .code("SKU-001")
                .name("Whey Protein 2kg")
                .price(BigDecimal.valueOf(850))
                .stock(10)
                .minStock(5)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void givenANewCode_whenCreate_thenSaveWithDefaults() {
        CreateProductCommand command = new CreateProductCommand(
                "SKU-001", "Whey Protein 2kg", null, null, BigDecimal.valueOf(850), null, null, null);
        when(productRepository.existsByCode("SKU-001")).thenReturn(false);
        when(productRepository.save(any(ProductEntity.class))).thenReturn(existingProduct);

        ProductEntity result = productService.create(command);

        assertEquals(existingProduct, result);
    }

    @Test
    void givenAnExistingCode_whenCreate_thenThrowConflictException() {
        when(productRepository.existsByCode("SKU-001")).thenReturn(true);
        CreateProductCommand command = new CreateProductCommand(
                "SKU-001", "Whey Protein 2kg", null, null, BigDecimal.valueOf(850), null, null, null);

        assertThrows(ConflictException.class, () -> productService.create(command));

        verify(productRepository, never()).save(any());
    }

    @Test
    void givenAnUnknownCategory_whenCreate_thenThrowNotFoundException() {
        when(productRepository.existsByCode("SKU-001")).thenReturn(false);
        when(categoryRepository.existsById(99L)).thenReturn(false);
        CreateProductCommand command = new CreateProductCommand(
                "SKU-001", "Whey Protein 2kg", 99L, null, BigDecimal.valueOf(850), null, null, null);

        assertThrows(NotFoundException.class, () -> productService.create(command));

        verify(productRepository, never()).save(any());
    }

    @Test
    void givenAnExistingId_whenGetById_thenReturnTheProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        ProductEntity result = productService.getById(1L);

        assertEquals(existingProduct, result);
    }

    @Test
    void givenAnUnknownId_whenGetById_thenThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getById(99L));
    }

    @Test
    void whenSearch_thenDelegateToTheRepository() {
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(existingProduct));

        List<ProductEntity> result = productService.search(null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void givenAnExistingProduct_whenUpdate_thenChangeItsDetails() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        UpdateProductCommand command =
                new UpdateProductCommand("Whey Protein 5kg", null, null, BigDecimal.valueOf(1800), null, 3);

        ProductEntity result = productService.update(1L, command);

        assertEquals("Whey Protein 5kg", result.getName());
        assertEquals(3, result.getMinStock());
    }

    @Test
    void givenAnExistingProduct_whenDeactivate_thenItBecomesInactive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        ProductEntity result = productService.deactivate(1L);

        assertEquals(false, result.isActive());
    }

    @Test
    void givenAPositiveQuantity_whenRegisterPurchase_thenIncreaseStockAndSaveTheMovement() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(inventoryMovementRepository.save(any(InventoryMovementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RegisterPurchaseCommand command = new RegisterPurchaseCommand(20, BigDecimal.valueOf(40), 1L);

        InventoryMovementEntity result = productService.registerPurchase(1L, command);

        assertEquals(30, existingProduct.getStock());
        assertEquals(30, result.getFinalStock());
        assertEquals(MovementType.PURCHASE, result.getType());
    }

    @Test
    void givenAZeroQuantity_whenRegisterPurchase_thenThrowBadRequestException() {
        RegisterPurchaseCommand command = new RegisterPurchaseCommand(0, null, 1L);

        assertThrows(BadRequestException.class, () -> productService.registerPurchase(1L, command));

        verify(productRepository, never()).findById(any());
    }

    @Test
    void givenAProductWithMovements_whenListMovements_thenReturnThem() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .id(1L).productId(1L).type(MovementType.PURCHASE).quantity(5).finalStock(15).createdAt(Instant.now())
                .build();
        when(inventoryMovementRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(movement));

        List<InventoryMovementEntity> result = productService.listMovements(1L);

        assertEquals(1, result.size());
    }
}
