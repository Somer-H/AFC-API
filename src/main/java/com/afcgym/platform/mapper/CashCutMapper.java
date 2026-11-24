package com.afcgym.platform.mapper;

import com.afcgym.platform.service.dto.CashCutReport;
import com.afcgym.platform.service.dto.PaymentMethodBreakdown;
import com.afcgym.platform.web.dto.response.CashCutResponse;
import com.afcgym.platform.web.dto.response.PaymentMethodBreakdownResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CashCutMapper {

    CashCutResponse toResponse(CashCutReport report);

    PaymentMethodBreakdownResponse toBreakdownResponse(PaymentMethodBreakdown breakdown);
}
