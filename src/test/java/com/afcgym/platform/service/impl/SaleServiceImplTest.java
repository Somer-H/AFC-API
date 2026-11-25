package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.InventoryMovementEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.persistance.entities.SaleItemEntity;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.MovementType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IInventoryMovementRepository;
import com.afcgym.platform.persistance.repositories.IProductRepository;
import com.afcgym.platform.persistance.repositories.ISaleRepository;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.dto.ExportedFile;
import com.afcgym.platform.service.dto.RegisterSaleCommand;
import com.afcgym.platform.service.dto.SaleLineCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IInventoryMovementRepository inventoryMovementRepository;

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private SaleServiceImpl saleService;

    private ProductEntity whey;
    private ProductEntity shaker;

    @BeforeEach
    void init() {
        whey = product(1L, "Whey Protein", "850.00", 10, true);
        shaker = product(2L, "Shaker", "150.00", 5, true);
    }

    @Test
    void givenNoLines_whenRegister_thenThrowBadRequestException() {
        RegisterSaleCommand command = new RegisterSaleCommand(List.of(), PaymentMethod.CASH, 1L);

        assertThrows(BadRequestException.class, () -> saleService.register(command));

        verify(saleRepository, never()).save(any());
    }

    @Test
    void givenAZeroQuantity_whenRegister_thenThrowBadRequestException() {
        RegisterSaleCommand command =
                new RegisterSaleCommand(List.of(new SaleLineCommand(1L, 0)), PaymentMethod.CASH, 1L);

        assertThrows(BadRequestException.class, () -> saleService.register(command));

        verify(productRepository, never()).findWithLockById(any());
    }

    @Test
    void givenAnUnknownProduct_whenRegister_thenThrowNotFoundException() {
        when(productRepository.findWithLockById(99L)).thenReturn(Optional.empty());
        RegisterSaleCommand command =
                new RegisterSaleCommand(List.of(new SaleLineCommand(99L, 1)), PaymentMethod.CASH, 1L);

        assertThrows(NotFoundException.class, () -> saleService.register(command));

        verify(saleRepository, never()).save(any());
    }

    @Test
    void givenAnInactiveProduct_whenRegister_thenThrowConflictException() {
        ProductEntity inactive = product(3L, "Barra vieja", "30.00", 10, false);
        when(productRepository.findWithLockById(3L)).thenReturn(Optional.of(inactive));
        RegisterSaleCommand command =
                new RegisterSaleCommand(List.of(new SaleLineCommand(3L, 1)), PaymentMethod.CASH, 1L);

        ConflictException exception = assertThrows(ConflictException.class, () -> saleService.register(command));

        assertEquals("El producto Barra vieja no está disponible", exception.getMessage());
    }

    @Test
    void givenMoreUnitsThanTheStock_whenRegister_thenThrowConflictWithTheAvailableStockAndChangeNothing() {
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(whey));
        when(productRepository.findWithLockById(2L)).thenReturn(Optional.of(shaker));
        RegisterSaleCommand command = new RegisterSaleCommand(
                List.of(new SaleLineCommand(1L, 2), new SaleLineCommand(2L, 6)), PaymentMethod.CASH, 1L);

        ConflictException exception = assertThrows(ConflictException.class, () -> saleService.register(command));

        assertEquals("Stock insuficiente para Shaker: disponible 5", exception.getMessage());
        assertEquals(10, whey.getStock());
        verify(inventoryMovementRepository, never()).save(any());
        verify(saleRepository, never()).save(any());
    }

    @Test
    void givenRepeatedLinesForOneProduct_whenRegister_thenAddTheirQuantitiesBeforeCheckingTheStock() {
        when(productRepository.findWithLockById(2L)).thenReturn(Optional.of(shaker));
        RegisterSaleCommand command = new RegisterSaleCommand(
                List.of(new SaleLineCommand(2L, 3), new SaleLineCommand(2L, 3)), PaymentMethod.CASH, 1L);

        assertThrows(ConflictException.class, () -> saleService.register(command));
    }

    @Test
    void givenEnoughStock_whenRegister_thenDiscountTheStockSaveTheMovementsAndAssignTheFolio() {
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(whey));
        when(productRepository.findWithLockById(2L)).thenReturn(Optional.of(shaker));
        when(saleRepository.save(any(SaleEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        RegisterSaleCommand command = new RegisterSaleCommand(
                List.of(new SaleLineCommand(2L, 1), new SaleLineCommand(1L, 1), new SaleLineCommand(1L, 1)),
                PaymentMethod.CARD,
                7L);

        SaleEntity result = saleService.register(command);

        assertEquals("V-000001", result.getFolio());
        assertEquals(0, new BigDecimal("1850.00").compareTo(result.getTotal()));
        assertEquals(PaymentMethod.CARD, result.getPaymentMethod());
        assertEquals(7L, result.getCashierId());
        assertEquals(2, result.getItems().size());
        assertEquals(2, result.getItems().getFirst().getQuantity());
        assertEquals(0, new BigDecimal("1700.00").compareTo(result.getItems().getFirst().getSubtotal()));
        assertEquals(8, whey.getStock());
        assertEquals(4, shaker.getStock());

        ArgumentCaptor<InventoryMovementEntity> movements = ArgumentCaptor.forClass(InventoryMovementEntity.class);
        verify(inventoryMovementRepository, times(2)).save(movements.capture());
        assertEquals(MovementType.SALE, movements.getValue().getType());
        assertEquals(4, movements.getValue().getFinalStock());
        assertEquals(7L, movements.getValue().getUserId());
    }

    @Test
    void givenEnoughStock_whenValidateStock_thenDoNotThrow() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(whey));

        assertDoesNotThrow(() -> saleService.validateStock(List.of(new SaleLineCommand(1L, 10))));

        assertEquals(10, whey.getStock());
    }

    @Test
    void givenMoreUnitsThanTheStock_whenValidateStock_thenThrowConflictException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(whey));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> saleService.validateStock(List.of(new SaleLineCommand(1L, 11))));

        assertEquals("Stock insuficiente para Whey Protein: disponible 10", exception.getMessage());
    }

    @Test
    void givenAStartAfterTheEnd_whenListByDateRange_thenThrowBadRequestException() {
        LocalDate today = LocalDate.now();

        assertThrows(BadRequestException.class, () -> saleService.listByDateRange(today, today.minusDays(1)));
    }

    @Test
    void givenAValidRange_whenListByDateRange_thenQueryFromTheStartOfTheFirstDayToTheStartOfTheDayAfterTheLast() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 15);
        ZoneId zone = ZoneId.systemDefault();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = LocalDate.of(2026, 11, 16).atStartOfDay(zone).toInstant();
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(start, end))
                .thenReturn(List.of());

        List<SaleEntity> result = saleService.listByDateRange(from, to);

        assertEquals(0, result.size());
    }

    @Test
    void givenAnUnknownId_whenGetById_thenThrowNotFoundException() {
        when(saleRepository.findWithItemsById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> saleService.getById(99L));
    }

    private ProductEntity product(Long id, String name, String price, int stock, boolean active) {
        return ProductEntity.builder()
                .id(id)
                .code("SKU-" + id)
                .name(name)
                .price(new BigDecimal(price))
                .stock(stock)
                .minStock(5)
                .active(active)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void givenSalesWithAndWithoutCashier_whenExport_thenLookUpTheCashierNamesAndNameTheFileAfterTheRange() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        SaleEntity withCashier = exportedSale(2L, "V-000002", 7L, "1850.00");
        SaleEntity withoutCashier = exportedSale(1L, "V-000001", null, "25.00");
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(withCashier, withoutCashier));
        when(userRepository.findAllById(Set.of(7L))).thenReturn(List.of(UserEntity.builder()
                .id(7L)
                .username("recepcion")
                .fullName("Recepción Uno")
                .email("recepcion@afcgym.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .build()));

        ExportedFile export = saleService.export(from, to);

        assertEquals("ventas_2026-11-01_2026-11-30.xlsx", export.fileName());
        assertTrue(export.content().length > 0);
    }

    private SaleEntity exportedSale(Long id, String folio, Long cashierId, String total) {
        return SaleEntity.builder()
                .id(id)
                .folio(folio)
                .total(new BigDecimal(total))
                .paymentMethod(PaymentMethod.CASH)
                .cashierId(cashierId)
                .createdAt(Instant.parse("2026-11-10T18:00:00Z"))
                .items(List.of(SaleItemEntity.builder()
                        .id(id)
                        .productId(1L)
                        .productName("Whey Protein")
                        .quantity(2)
                        .unitPrice(new BigDecimal(total))
                        .subtotal(new BigDecimal(total))
                        .build()))
                .build();
    }

    private SaleEntity withId(SaleEntity sale) {
        if (sale.getId() != null) {
            return sale;
        }
        return SaleEntity.builder()
                .id(1L)
                .folio(sale.getFolio())
                .total(sale.getTotal())
                .paymentMethod(sale.getPaymentMethod())
                .cashierId(sale.getCashierId())
                .createdAt(sale.getCreatedAt())
                .items(sale.getItems())
                .build();
    }
}
