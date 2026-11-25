package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;

public record AppointmentAgendaItem(PaymentAppointmentEntity appointment, PaymentEntity payment, MemberEntity member) {}
