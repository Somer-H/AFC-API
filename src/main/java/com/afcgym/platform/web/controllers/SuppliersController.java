package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.SupplierMapper;
import com.afcgym.platform.service.ISupplierService;
import com.afcgym.platform.service.dto.CreateSupplierCommand;
import com.afcgym.platform.web.dto.request.CreateSupplierRequest;
import com.afcgym.platform.web.dto.response.SupplierResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers")
public class SuppliersController {

    private final ISupplierService supplierService;
    private final SupplierMapper supplierMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a supplier")
    public SupplierResponse create(@Valid @RequestBody CreateSupplierRequest request) {
        return supplierMapper.toResponse(supplierService.create(
                new CreateSupplierCommand(request.name(), request.contact(), request.phone())));
    }

    @GetMapping
    @Operation(summary = "List suppliers")
    public List<SupplierResponse> list() {
        return supplierService.list().stream().map(supplierMapper::toResponse).toList();
    }
}
