package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.service.dto.BankTransferDetails;
import com.afcgym.platform.service.dto.MemberPaymentView;
import com.afcgym.platform.utils.PaymentReferenceFormatter;
import com.afcgym.platform.web.dto.response.BankTransferDetailsResponse;
import com.afcgym.platform.web.dto.response.MemberPaymentResponse;
import com.afcgym.platform.web.dto.response.PaymentAppointmentResponse;
import com.afcgym.platform.web.dto.response.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = PaymentReferenceFormatter.class)
public interface PaymentMapper {

    @Mapping(target = "reference", expression = "java(PaymentReferenceFormatter.reference(entity.getId()))")
    PaymentResponse toResponse(PaymentEntity entity);

    @Mapping(target = "id", source = "payment.id")
    @Mapping(target = "reference", expression = "java(PaymentReferenceFormatter.reference(view.payment().getId()))")
    @Mapping(target = "membershipType", source = "payment.membershipType")
    @Mapping(target = "method", source = "payment.method")
    @Mapping(target = "status", source = "payment.status")
    @Mapping(target = "amount", source = "payment.amount")
    @Mapping(target = "discountPercent", source = "payment.discountPercent")
    @Mapping(target = "createdAt", source = "payment.createdAt")
    @Mapping(target = "approvedAt", source = "payment.approvedAt")
    @Mapping(target = "rejectionReason", source = "payment.rejectionReason")
    @Mapping(target = "checkoutUrl", source = "payment.checkoutUrl")
    @Mapping(target = "bankTransfer", source = "bankTransfer")
    @Mapping(target = "appointment", source = "appointment")
    MemberPaymentResponse toMemberResponse(MemberPaymentView view);

    BankTransferDetailsResponse toBankTransferResponse(BankTransferDetails details);

    PaymentAppointmentResponse toAppointmentResponse(PaymentAppointmentEntity appointment);
}
