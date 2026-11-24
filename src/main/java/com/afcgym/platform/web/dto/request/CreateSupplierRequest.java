package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
        @NotBlank String name,
        String contact,
        String phone
) {}
