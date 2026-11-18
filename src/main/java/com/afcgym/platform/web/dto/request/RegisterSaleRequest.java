package com.afcgym.platform.web.dto.request;

import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RegisterSaleRequest(
        @NotEmpty List<@NotNull @Valid SaleLineRequest> items,
        @NotNull PaymentMethod paymentMethod
) {}
