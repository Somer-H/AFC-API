package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.web.dto.response.PaymentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(PaymentEntity entity);
}
