package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaymentRequest(@NotBlank @Size(max = 500) String reason) {}
