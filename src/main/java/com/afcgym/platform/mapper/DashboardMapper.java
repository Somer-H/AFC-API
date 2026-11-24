package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.service.dto.DashboardSummary;
import com.afcgym.platform.web.dto.response.DashboardSummaryResponse;
import java.time.YearMonth;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    DashboardSummaryResponse toResponse(DashboardSummary summary);

    @Mapping(target = "expiringMemberships", source = "expiringMembers")
    DashboardSummaryResponse.Alerts toAlertsResponse(DashboardSummary.Alerts alerts);

    @Mapping(target = "daysLeft",
            expression = "java(java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), member.getDueDate()))")
    DashboardSummaryResponse.ExpiringMember toExpiringMember(MemberEntity member);

    DashboardSummaryResponse.LowStockProduct toLowStockProduct(ProductEntity product);

    default String toText(YearMonth month) {
        return month.toString();
    }
}
