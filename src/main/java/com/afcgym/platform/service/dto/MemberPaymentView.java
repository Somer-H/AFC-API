package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;

public record MemberPaymentView(
        PaymentEntity payment,
        PaymentAppointmentEntity appointment,
        BankTransferDetails bankTransfer) {}
