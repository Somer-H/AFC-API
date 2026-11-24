package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.PushTokenEntity;
import com.afcgym.platform.service.dto.AppointmentAcceptedEvent;
import com.afcgym.platform.service.dto.AppointmentCancelledEvent;
import com.afcgym.platform.service.dto.MembershipRenewedEvent;
import com.afcgym.platform.service.dto.PaymentRejectedEvent;
import com.afcgym.platform.service.dto.Requester;
import java.time.LocalDate;

public interface INotificationService {

    PushTokenEntity registerPushToken(String token, Requester requester);

    int sendExpiryReminders(LocalDate today, int daysBefore);

    void notifyMembershipRenewed(MembershipRenewedEvent event);

    void notifyPaymentRejected(PaymentRejectedEvent event);

    void notifyAppointmentAccepted(AppointmentAcceptedEvent event);

    void notifyAppointmentCancelled(AppointmentCancelledEvent event);
}
