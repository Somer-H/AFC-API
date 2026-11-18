package com.afcgym.platform.web.dto.response;

import lombok.Builder;

@Builder
public record SupplierResponse(Long id, String name, String contact, String phone) {}
