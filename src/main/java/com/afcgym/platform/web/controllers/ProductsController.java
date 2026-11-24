package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.InventoryMovementMapper;
import com.afcgym.platform.mapper.ProductMapper;
import com.afcgym.platform.service.IProductService;
import com.afcgym.platform.service.dto.CreateProductCommand;
import com.afcgym.platform.service.dto.RegisterPurchaseCommand;
import com.afcgym.platform.service.dto.UpdateProductCommand;
import com.afcgym.platform.web.dto.request.CreateProductRequest;
import com.afcgym.platform.web.dto.request.RegisterPurchaseRequest;
import com.afcgym.platform.web.dto.request.UpdateProductRequest;
import com.afcgym.platform.web.dto.response.InventoryMovementResponse;
import com.afcgym.platform.web.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductsController {

    private final IProductService productService;
    private final ProductMapper productMapper;
    private final InventoryMovementMapper inventoryMovementMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productMapper.toResponse(productService.create(new CreateProductCommand(
                request.code(),
                request.name(),
                request.categoryId(),
                request.supplierId(),
                request.price(),
                request.cost(),
                request.stock(),
                request.minStock())));
    }

    @GetMapping
    @Operation(summary = "Search products by name or code, optionally filtered by low stock")
    public List<ProductResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean lowStock) {
        return productService.search(search, lowStock).stream().map(productMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ProductResponse getById(@PathVariable Long id) {
        return productMapper.toResponse(productService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productMapper.toResponse(productService.update(id, new UpdateProductCommand(
                request.name(),
                request.categoryId(),
                request.supplierId(),
                request.price(),
                request.cost(),
                request.minStock())));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a product")
    public ProductResponse deactivate(@PathVariable Long id) {
        return productMapper.toResponse(productService.deactivate(id));
    }

    @PostMapping("/{id}/inventory-movements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an inventory purchase for a product")
    public InventoryMovementResponse registerPurchase(
            @PathVariable Long id,
            @Valid @RequestBody RegisterPurchaseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromJwt(jwt);
        return inventoryMovementMapper.toResponse(productService.registerPurchase(id, new RegisterPurchaseCommand(
                request.quantity(),
                request.unitCost(),
                authenticatedUser.userId())));
    }

    @GetMapping("/{id}/inventory-movements")
    @Operation(summary = "List a product's inventory movement history")
    public List<InventoryMovementResponse> listMovements(@PathVariable Long id) {
        return productService.listMovements(id).stream().map(inventoryMovementMapper::toResponse).toList();
    }
}
