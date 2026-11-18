package com.afcgym.platform.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ValidateStockRequest(
        @NotEmpty List<@NotNull @Valid SaleLineRequest> items
) {}
