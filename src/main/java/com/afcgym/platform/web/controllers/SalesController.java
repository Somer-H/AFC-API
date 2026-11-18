package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.SaleMapper;
import com.afcgym.platform.service.ISaleService;
import com.afcgym.platform.service.dto.RegisterSaleCommand;
import com.afcgym.platform.service.dto.SaleLineCommand;
import com.afcgym.platform.web.dto.request.RegisterSaleRequest;
import com.afcgym.platform.web.dto.request.SaleLineRequest;
import com.afcgym.platform.web.dto.request.ValidateStockRequest;
import com.afcgym.platform.web.dto.response.SaleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales")
public class SalesController {

    private final ISaleService saleService;
    private final SaleMapper saleMapper;

    @PostMapping("/stock-validation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Check that every cart line fits the available stock without registering a sale")
    public void validateStock(@Valid @RequestBody ValidateStockRequest request) {
        saleService.validateStock(toLines(request.items()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a sale and discount the stock of every product atomically")
    public SaleResponse register(@Valid @RequestBody RegisterSaleRequest request, @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromJwt(jwt);
        return saleMapper.toResponse(saleService.register(new RegisterSaleCommand(
                toLines(request.items()),
                request.paymentMethod(),
                authenticatedUser.userId())));
    }

    @GetMapping
    @Operation(summary = "List the sales registered between two dates, both inclusive")
    public List<SaleResponse> listByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return saleService.listByDateRange(from, to).stream().map(saleMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a sale with its items")
    public SaleResponse getById(@PathVariable Long id) {
        return saleMapper.toResponse(saleService.getById(id));
    }

    private List<SaleLineCommand> toLines(List<SaleLineRequest> items) {
        return items.stream().map(item -> new SaleLineCommand(item.productId(), item.quantity())).toList();
    }
}
