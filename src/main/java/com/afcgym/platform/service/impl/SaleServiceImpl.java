package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.InventoryMovementEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.persistance.entities.SaleItemEntity;
import com.afcgym.platform.persistance.entities.enums.MovementType;
import com.afcgym.platform.persistance.repositories.IInventoryMovementRepository;
import com.afcgym.platform.persistance.repositories.IProductRepository;
import com.afcgym.platform.persistance.repositories.ISaleRepository;
import com.afcgym.platform.service.ISaleService;
import com.afcgym.platform.service.dto.RegisterSaleCommand;
import com.afcgym.platform.service.dto.SaleLineCommand;
import com.afcgym.platform.utils.SecureTokenGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements ISaleService {

    private static final String FOLIO_PREFIX = "V-";
    private static final String TEMP_FOLIO_PREFIX = "TMP-";

    private final ISaleRepository saleRepository;
    private final IProductRepository productRepository;
    private final IInventoryMovementRepository inventoryMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public void validateStock(List<SaleLineCommand> lines) {
        groupQuantitiesByProduct(lines).forEach((productId, quantity) -> {
            ProductEntity product = productRepository.findById(productId).orElseThrow(this::productNotFound);
            ensureAvailable(product, quantity);
        });
    }

    @Override
    @Transactional
    public SaleEntity register(RegisterSaleCommand command) {
        Map<Long, Integer> quantities = groupQuantitiesByProduct(command.lines());
        List<ProductEntity> products = new ArrayList<>();
        List<SaleItemEntity> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductEntity product = productRepository.findWithLockById(entry.getKey())
                    .orElseThrow(this::productNotFound);
            int quantity = entry.getValue();
            ensureAvailable(product, quantity);
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            items.add(SaleItemEntity.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build());
            products.add(product);
            total = total.add(subtotal);
        }

        Instant now = Instant.now();
        for (ProductEntity product : products) {
            int quantity = quantities.get(product.getId());
            product.decreaseStock(quantity);
            inventoryMovementRepository.save(InventoryMovementEntity.builder()
                    .productId(product.getId())
                    .type(MovementType.SALE)
                    .quantity(quantity)
                    .finalStock(product.getStock())
                    .userId(command.cashierId())
                    .createdAt(now)
                    .build());
        }

        SaleEntity sale = SaleEntity.builder()
                .folio(TEMP_FOLIO_PREFIX + SecureTokenGenerator.generate(8))
                .total(total)
                .paymentMethod(command.paymentMethod())
                .cashierId(command.cashierId())
                .createdAt(now)
                .items(items)
                .build();
        sale = saleRepository.save(sale);
        sale.assignFolio(FOLIO_PREFIX + String.format("%06d", sale.getId()));
        return saleRepository.save(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleEntity> listByDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("La fecha inicial no puede ser posterior a la final");
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        return saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleEntity getById(Long id) {
        return saleRepository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("No se encontró la venta solicitada"));
    }

    private Map<Long, Integer> groupQuantitiesByProduct(List<SaleLineCommand> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("La venta debe tener al menos un producto");
        }
        Map<Long, Integer> quantities = new TreeMap<>();
        for (SaleLineCommand line : lines) {
            if (line.productId() == null) {
                throw new BadRequestException("Cada producto de la venta debe indicar su identificador");
            }
            if (line.quantity() <= 0) {
                throw new BadRequestException("La cantidad debe ser mayor a cero");
            }
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }
        return quantities;
    }

    private void ensureAvailable(ProductEntity product, int quantity) {
        if (!product.isActive()) {
            throw new ConflictException("El producto " + product.getName() + " no está disponible");
        }
        if (product.getStock() < quantity) {
            throw new ConflictException(
                    "Stock insuficiente para " + product.getName() + ": disponible " + product.getStock());
        }
    }

    private NotFoundException productNotFound() {
        return new NotFoundException("No se encontró el producto solicitado");
    }
}
