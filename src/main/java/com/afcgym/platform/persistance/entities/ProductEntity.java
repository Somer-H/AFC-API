package com.afcgym.platform.persistance.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false, length = 150)
    private String name;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "category_id")
    private Long categoryId;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Setter(AccessLevel.PRIVATE)
    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false)
    private int stock;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "min_stock", nullable = false)
    private int minStock;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public void updateDetails(
            String name, Long categoryId, Long supplierId, BigDecimal price, BigDecimal cost, int minStock) {
        setName(name);
        setCategoryId(categoryId);
        setSupplierId(supplierId);
        setPrice(price);
        setCost(cost);
        setMinStock(minStock);
    }

    public void increaseStock(int quantity) {
        setStock(stock + quantity);
    }

    public void decreaseStock(int quantity) {
        setStock(stock - quantity);
    }

    public void deactivate() {
        setActive(false);
    }
}
