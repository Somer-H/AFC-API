package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.service.dto.AppointmentAgendaItem;
import com.afcgym.platform.service.dto.CancelAppointmentCommand;
import java.time.LocalDate;
import java.util.List;

public interface IPaymentAppointmentService {

    List<AppointmentAgendaItem> listAgenda(LocalDate today);

    PaymentAppointmentEntity accept(Long appointmentId, Long adminUserId);

    PaymentAppointmentEntity cancel(Long appointmentId, CancelAppointmentCommand command, Long adminUserId);
}
