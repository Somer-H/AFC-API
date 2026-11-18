package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import java.util.List;

public record RegisterSaleCommand(List<SaleLineCommand> lines, PaymentMethod paymentMethod, Long cashierId) {}
