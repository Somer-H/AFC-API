package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.service.dto.AppointmentAgendaItem;
import com.afcgym.platform.utils.PaymentReferenceFormatter;
import com.afcgym.platform.web.dto.response.AppointmentAgendaResponse;
import com.afcgym.platform.web.dto.response.PaymentAppointmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = PaymentReferenceFormatter.class)
public interface PaymentAppointmentMapper {

    PaymentAppointmentResponse toResponse(PaymentAppointmentEntity appointment);

    @Mapping(target = "id", source = "appointment.id")
    @Mapping(target = "scheduledAt", source = "appointment.scheduledAt")
    @Mapping(target = "status", source = "appointment.status")
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "paymentReference", expression = "java(PaymentReferenceFormatter.reference(item.payment().getId()))")
    @Mapping(target = "paymentStatus", source = "payment.status")
    @Mapping(target = "membershipType", source = "payment.membershipType")
    @Mapping(target = "amount", source = "payment.amount")
    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "memberCode", source = "member.code")
    @Mapping(target = "memberName", source = "member.fullName")
    @Mapping(target = "memberPhone", source = "member.phone")
    AppointmentAgendaResponse toAgendaResponse(AppointmentAgendaItem item);
}
